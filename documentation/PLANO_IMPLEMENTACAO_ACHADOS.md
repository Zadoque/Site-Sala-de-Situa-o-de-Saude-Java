# Plano incremental para os achados de arquitetura, segurança e testes

Este plano transforma a revisão comparativa entre a `main` original e a
branch de referência em incrementos implementáveis. A referência de Artur é
usada como fonte de ideias, não como fonte para copiar código ou migrações sem
compatibilidade com o banco existente.

## Regras de execução

Cada incremento deve:

1. tratar uma regra ou risco claramente definido;
2. alterar código e testes na mesma etapa;
3. preferir resultado observável: resposta HTTP, estado persistido ou efeito
   externo;
4. executar `./mvnw clean verify`;
5. executar PIT quando a mudança alterar regra de domínio ou segurança;
6. atualizar esta documentação com o resultado;
7. gerar um commit semântico isolado.

Testes que verificam somente chamadas internas não são suficientes quando a
regra pode ser observada no estado final ou na resposta da aplicação.

## P0 — segurança e consistência do ciclo de vida

### P0.1 — impedir reativação indevida por primeiro acesso ou reset

Status: concluído no incremento `fix(auth): preserve administrative account blocks`.

- manter `ativo` como decisão administrativa de acesso;
- primeiro acesso configura nome e senha, mas não desbloqueia uma conta;
- reset de senha altera a credencial, mas não desbloqueia uma conta;
- garantir que conta desativada continue sem login e sem refresh;
- adicionar testes unitários e de integração para o estado persistido.

Critério de aceite: uma conta desativada permanece `ativo=false` depois de
concluir primeiro acesso ou reset e suas tentativas de autenticação falham.
Os testes unitários e de integração confirmam que o estado desativado não é
alterado.

### P0.2 — consumo único de tokens sob concorrência

- adicionar consulta com lock pessimista ou atualização condicional atômica;
- manter validação, alteração da credencial e consumo na mesma transação;
- rejeitar a segunda requisição concorrente sem alterar novamente a conta;
- testar no PostgreSQL.

Critério de aceite: duas requisições simultâneas com o mesmo token produzem
exatamente uma conclusão bem-sucedida.

### P0.3 — identidade imutável no JWT

- usar o ID do usuário como `sub`;
- carregar o usuário pelo ID no filtro;
- definir estratégia de transição para tokens emitidos com e-mail;
- avaliar versão de credencial para invalidar access tokens após reset.

Critério de aceite: reutilizar um e-mail não reutiliza a identidade de um JWT
antigo e tokens antigos seguem a política de transição documentada.

### P0.4 — revogação consistente de sessões

- revogar refresh sessions ao concluir reset;
- garantir persistência da revogação quando um token expirado ou reutilizado
  gerar erro;
- testar rotação concorrente e reutilização de refresh.

Critério de aceite: reset invalida sessões anteriores e a revogação não é
desfeita pelo rollback da resposta de erro.

### P0.5 — geração de tokens testável

- extrair uma abstração de geração segura de tokens;
- manter `SecureRandom` na produção;
- usar fonte determinística somente nos testes;
- testar que os bytes aleatórios influenciam o token e o hash persistido.

Critério de aceite: a remoção da geração de bytes faz um teste de segurança
falhar sem depender de probabilidade.

### P0.6 — probes de saúde funcionais

- liberar liveness e readiness explicitamente;
- manter endpoints de negócio protegidos;
- iniciar a imagem com dependências de teste;
- executar smoke test real dos endpoints de saúde.

Critério de aceite: o healthcheck configurado no Docker passa em uma instância
saudável sem credenciais e endpoints de negócio continuam protegidos.

## P1 — testes que comprovam comportamento

### P1.1 — login completo

Cobrir PostgreSQL, Flyway, senha codificada, login HTTP, JWT retornado,
`/auth/me`, autorização administrativa, senha inválida, usuário desativado,
token expirado e assinatura inválida.

### P1.2 — qualidade dos testes unitários

Preservar testes de regras úteis, mas usar mapper real quando necessário,
capturar entidades persistidas, conferir campos alterados, verificar ausência
de efeitos e reduzir mocks sem comportamento.

### P1.3 — convites e administração

Documentar e testar convidar, renovar, cancelar, expirar, desativar usuários,
proteger o último administrador e definir a política para administradores.

### P1.4 — reset voluntário e administrativo

Separar os dois fluxos, verificar a senha com `matches`, definir quando o
login é bloqueado e revogar sessões de acordo com P0.4.

### P1.5 — unicidade normalizada de e-mail

Auditar colisões, normalizar dados e criar índice único compatível com a
normalização adotada. Testar convites concorrentes no PostgreSQL.

### P1.6 — contrato web

Alinhar rotas, cookie, CORS (`PATCH`), CSRF, `HttpOnly`, `Secure`, `SameSite`,
expiração e remoção do cookie.

### P1.7 — rate limit e e-mail

Limpar o mapa de tentativas, documentar o limite single-node ou migrá-lo para
um armazenamento compartilhado, e definir envio de e-mail após commit.

## P2 — migrações e arquitetura

- preservar migrações aplicadas e criar somente versões novas;
- testar banco vazio e atualização de banco existente no PostgreSQL;
- escolher uma única representação de permissão;
- proteger Controller → Service → Repository com ArchUnit;
- garantir que contratos documentados tenham validação automatizada.

## P3 — analytics

- validar o schema real do pipeline;
- separar ausência de publicação de zero casos;
- resolver opções de filtro anunciadas e não aceitas;
- testar agregações, filtros combinados e cobertura territorial no PostgreSQL.

## P4 — qualidade e operação

- manter JaCoCo em 90%/75%/88% e PIT em 80% como baseline;
- ampliar PIT somente quando o escopo estiver estável;
- publicar relatórios no CI;
- testar imagem, smoke test, backup, restauração, rollback e observabilidade
  em homologação.

## Ordem dos incrementos

1. P0.1 — estado administrativo não é reativado por token.
2. P0.4 — reset revoga sessões.
3. P0.2 — token de uso único sob concorrência.
4. P0.6 — readiness/liveness reais.
5. P1.1 — login completo com PostgreSQL e Flyway.
6. P0.3 — identidade imutável no JWT.
7. P0.5 — geração de tokens injetável.
8. P1.5 — unicidade real de e-mail.
9. P1.6/P1.7 — contrato web, rate limit e e-mail.
10. P2/P3 — migrações, arquitetura e analytics.

Não serão usados incrementos de cobertura como objetivo isolado. A cobertura
deve crescer como consequência de testes que protegem riscos concretos.

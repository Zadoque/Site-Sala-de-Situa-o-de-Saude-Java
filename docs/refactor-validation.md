# Validação da refatoração de pacotes

Branch: `feat/national-coverage-validation`  
Base registrada: `97b2f0c601553eadd8e5de4541ea4218ec1197b0`

## Commits da refatoração

- `ac698e6` — reorganização dos pacotes.
- `f2225f7` — isolamento dos DTOs HTTP dos Services e mappers internos.
- `d8c5a57` — testes adicionais de cobertura de mutações.

## Verificações

| Verificação | Resultado | Evidência/observação |
| --- | --- | --- |
| Surefire | Aprovado | 121 testes, 0 falhas, 0 erros, 0 ignorados |
| ArchUnit | Aprovado | Dependências, localização de Services/Repositories/Controllers, configuração e ausência de pacotes legados |
| Spotless | Aprovado | `./mvnw -q spotless:check` |
| Checkstyle | Aprovado | `./mvnw -q checkstyle:check` |
| PMD | Aprovado | `./mvnw -q pmd:check` |
| PIT | Aprovado | 111 mutações, 92 eliminadas, 83% de score; limiar configurado: 80% |
| Failsafe limpo | Aprovado | Executado pelo `nss-deployment` com `deployment.env`, rede Docker interna e socket Docker montado |

## Failsafe e ambiente

A execução limpa foi feita com PostgreSQL, Redis e Testcontainers isolados. Uma repetição posterior no banco persistente do deployment falhou por dados de execuções anteriores (`duplicate key` e sessões ativas); essa execução não é usada como aprovação funcional.

Os testes Redis e PostgreSQL não usam bancos ou serviços de produção. O cache analítico mantém a leitura de JSON produzido pelo modelo de resposta anterior, com teste de compatibilidade em `AnalyticsResponseCacheTest`.

## Garantias arquiteturais verificadas

- Services não importam `dto.request`, `dto.response` ou `dto.mapper`.
- `JwtService` e `CustomUserDetailsService` são as únicas exceções explícitas de `@Service` em `user.security`.
- `SecurityConfig` é a exceção nominal para `@Configuration` fora de `config`.
- Controllers REST ficam em `controller` e Repositories anotados ficam em `repository`.
- Nenhuma classe permanece nos pacotes legados `api`, `application`, `domain`, `infrastructure` ou `web`.
- `pendingInvitations()` materializa os dados necessários dentro da transação JPA.

## Limitações

Não foi feita comparação automatizada de snapshots HTTP antes/depois. A equivalência é coberta pelos contratos e testes de integração existentes, mas uma comparação formal de respostas continua recomendável antes do merge final.

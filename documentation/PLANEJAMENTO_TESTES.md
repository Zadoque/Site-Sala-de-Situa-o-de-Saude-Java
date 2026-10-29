# Planejamento incremental de testes

## Objetivo

Aumentar a confiança no backend com testes úteis, evitando metas artificiais.
Cada ciclo deve elevar em aproximadamente 5 pontos percentuais a cobertura
global de um contador, priorizando regras de negócio e fluxos de risco.

## Linha de base inicial

Medição feita com JaCoCo sobre os 28 testes executados pelo Maven:

| Métrica | Cobertura atual | Próxima meta (+5 p.p.) |
| --- | ---: | ---: |
| Linhas | 48,29% | 53,29% |
| Branches | 41,67% | 46,67% |
| Métodos | 46,80% | 51,80% |
| Instruções | 47,76% | 52,76% |
| Complexidade | 36,56% | 41,56% |

O JaCoCo gera os relatórios em `target/site/jacoco/` e o build impede
regressões abaixo da linha de base atual para linhas, branches e métodos.

Após o início do ciclo de API e persistência, a medição global chegou a
71,55% de linhas, 59,13% de branches e 67,98% de métodos, superando a
referência de 5 p.p. do ciclo anterior nos três contadores.

## Classificação dos testes

Cobertura de linhas, branches e métodos é medida pelo JaCoCo. “Teste unitário”
e “teste de integração” são categorias de execução e terão relatórios
separados, pois não são contadores de cobertura.

Estado atual do inventário:

- 24 testes unitários com JUnit/Mockito;
- 1 teste de contexto Spring Boot (`NssApplicationIT`), tratado como
  integração;
- 3 regras ArchUnit, tratadas como testes arquiteturais.

O primeiro ciclo separou os testes com Maven Surefire/Failsafe e classes `*IT`.
As coberturas por execução são:

| Execução | Linhas | Branches | Métodos |
| --- | ---: | ---: | ---: |
| Unitários/arquitetura | 39,82% | 39,29% | 36,45% |
| Integração | 13,78% | 5,16% | 15,27% |
| Global | 48,29% | 41,67% | 46,80% |

Os relatórios ficam em `target/site/jacoco/unit/`,
`target/site/jacoco/integration/` e `target/site/jacoco/`.

O ciclo de separação está concluído; os próximos ciclos começam pelo aumento
de cobertura comportamental, sem transformar a diferença entre unitário e
integração em uma meta artificial.

## Ciclos incrementais

### Ciclo 1 — baseline e separação

- manter JaCoCo no `verify`;
- gerar HTML, XML e CSV;
- separar Surefire e Failsafe;
- classificar testes existentes sem duplicá-los;
- registrar a cobertura unitária e de integração individualmente.

### Ciclo 2 — autenticação (+5 p.p.)

- testar login inválido e rate limiting;
- testar refresh, rotação e revogação;
- testar primeiro acesso e reset de senha;
- testar autorização administrativa e respostas HTTP.

Status: concluído. A cobertura global passou para 58,15% de linhas, 47,22% de
branches e 52,22% de métodos. O ciclo adicionou cenários de falha de login,
principal inválido, refresh, logout, rotação de sessão, primeiro acesso e
carregamento do usuário autenticado.

Próxima referência para o ciclo seguinte: 63,15% de linhas, 52,22% de
branches e 57,22% de métodos.

### Ciclo 3 — analytics (+5 p.p.)

- cobrir filtros por doença, período, sexo e faixa etária;
- cobrir município, distrito e bairro;
- cobrir estados `AVAILABLE`, `PARTIAL` e `UNAVAILABLE`;
- validar SQL parametrizado com PostgreSQL via Testcontainers.

Status: concluído. Foram adicionados testes para serviço, controllers, filtros,
repositórios e estados de disponibilidade. O ciclo elevou a cobertura global
para 63,72% de linhas, 52,38% de branches e 60,10% de métodos, superando a
meta de 5 p.p. em relação ao ciclo de autenticação.

Observação: o cenário de persistência foi validado com H2 compatível com o SQL
parametrizado atual. A validação com PostgreSQL via Testcontainers permanece
como parte do ciclo de API e persistência, para evitar duplicar testes sem
ganho comportamental nesta etapa.

Próxima referência para o ciclo seguinte: 68,72% de linhas, 57,38% de
branches e 65,10% de métodos.

### Ciclo 4 — API e persistência (+5 p.p.)

- adicionar testes `@WebMvcTest` para contratos HTTP;
- adicionar testes de repository com PostgreSQL real;
- validar erros de validação, autenticação e autorização;
- preservar o contrato JSON da API NSS v1.

Execução iniciada com cobertura do `AdminUserService`. O próximo incremento
deve priorizar contratos HTTP e persistência real, sempre validando respostas
observáveis e regras de autorização.

O incremento de cobertura está concluído, mas o ciclo permanece aberto: ainda
faltam testes `@WebMvcTest`, PostgreSQL real e a análise das mutações
sobreviventes do PIT.

Após os testes adicionais de sessões, rate limiting, catálogo territorial e
contratos de controller, a cobertura global chegou a 74,34% de linhas, 67,06%
de branches e 70,94% de métodos. O PIT também passou com 115 de 191 mutações
mortas (60%) e test strength de 91%. As 65 mutações sem cobertura permanecem
indicadores para a próxima análise; não foram ocultadas por exclusões amplas.

Com a execução HTTP e PostgreSQL real, a medição oficial do `clean verify`
passou a ser 77,12% de linhas, 69,05% de branches e 74,38% de métodos. Os
relatórios separados registraram 67,64%/64,68%/64,04% para unitários e
21,11%/14,29%/21,18% para integração.

Após os testes de `UserService`, `MailService`, primeiro acesso, reset de senha,
renovação de convite e cenários analíticos adicionais, a medição oficial mais
recente chegou a 88,75% de linhas, 74,60% de branches e 85,22% de métodos.
Unitários ficaram em 78,63%/69,84%/73,40% e integração em
22,00%/15,87%/23,15%.

Também foram adicionados três testes `@WebMvcTest` com `MockMvc` e um teste
`EpidemiologyRepositoryPostgresIT` com PostgreSQL 16 via Testcontainers. O
contrato HTTP e a persistência real estão cobertos; autorização completa por
JWT permanece como próximo cenário, pois os testes HTTP atuais desabilitam os
filtros para isolar o contrato do controller.

Esse cenário também foi validado com `AdminUserAuthorizationIT` usando a
aplicação completa: acesso anônimo retorna 401, usuário sem role ADMIN retorna
403 e usuário ADMIN acessa o endpoint. O ciclo 4 agora está concluído quanto
aos itens de API, persistência e autorização previstos.

### Ciclo 5 — força dos testes

- executar PIT nos services de autenticação e analytics;
- analisar mutantes sobreviventes;
- criar testes somente para comportamentos que os mutantes demonstrarem não
  estar protegidos.

Critério definido em `documentation/CRITERIOS_QUALIDADE_TESTES.md`. A primeira
execução do PIT encontrou 191 mutações, matou 84 (44%) e registrou test
strength de 74%; portanto o gate falhou corretamente e há mutações a analisar
antes de considerar esta fase concluída.

Depois dos ciclos de reforço, o PIT passou a matar 155 de 191 mutações (81%),
com test strength de 89%. A revisão dos testes encontrou uma lacuna real no
teste de convite: ele verificava um objeto de preparação, e não a entidade
persistida. O teste foi reescrito para capturar a entidade real e foram
adicionados cenários de reset, expiração, convite concluído, usuário ausente,
hash e resposta indisponível.

O resultado atual do PIT é 180 de 191 mutações mortas (94%), test strength de
96% e três mutações sem cobertura. As oito sobreviventes e as três sem
cobertura foram classificadas em `documentation/PIT_CLASSIFICACAO_MUTACOES.md`.
As classificadas como equivalentes, irrelevantes ou de infraestrutura não foram
transformadas em testes artificiais nem ocultadas por exclusões amplas.
O ciclo 5 está concluído.

## Critérios de aceitação

- cada ciclo aumenta a métrica planejada em aproximadamente 5 p.p.;
- nenhum teste é criado apenas para executar uma linha sem verificar um
  comportamento observável;
- `./mvnw verify` permanece verde;
- cobertura global não pode regredir abaixo da linha de base protegida;
- os relatórios unitário, integração e global são publicados no CI.

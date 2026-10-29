# Planejamento incremental de testes

## Objetivo

Aumentar a confiança no backend com testes úteis, evitando metas artificiais.
Cada ciclo deve elevar em aproximadamente 5 pontos percentuais a cobertura
global de um contador, priorizando regras de negócio e fluxos de risco.

## Linha de base atual

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

## Classificação dos testes

Cobertura de linhas, branches e métodos é medida pelo JaCoCo. “Teste unitário”
e “teste de integração” são categorias de execução e terão relatórios
separados, pois não são contadores de cobertura.

Estado atual do inventário:

- 24 testes unitários com JUnit/Mockito;
- 1 teste de contexto Spring Boot (`DemoApplicationTests`), tratado como
  integração;
- 3 regras ArchUnit, tratadas como testes arquiteturais.

A próxima etapa deve separar os testes de integração com Maven Failsafe e
classes `*IT`, permitindo gerar uma execução JaCoCo para unitários e outra para
integração.

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

### Ciclo 3 — analytics (+5 p.p.)

- cobrir filtros por doença, período, sexo e faixa etária;
- cobrir município, distrito e bairro;
- cobrir estados `AVAILABLE`, `PARTIAL` e `UNAVAILABLE`;
- validar SQL parametrizado com PostgreSQL via Testcontainers.

### Ciclo 4 — API e persistência (+5 p.p.)

- adicionar testes `@WebMvcTest` para contratos HTTP;
- adicionar testes de repository com PostgreSQL real;
- validar erros de validação, autenticação e autorização;
- preservar o contrato JSON da API NSS v1.

### Ciclo 5 — força dos testes

- executar PIT nos services de autenticação e analytics;
- analisar mutantes sobreviventes;
- criar testes somente para comportamentos que os mutantes demonstrarem não
  estar protegidos.

## Critérios de aceitação

- cada ciclo aumenta a métrica planejada em aproximadamente 5 p.p.;
- nenhum teste é criado apenas para executar uma linha sem verificar um
  comportamento observável;
- `./mvnw verify` permanece verde;
- cobertura global não pode regredir abaixo da linha de base protegida;
- os relatórios unitário, integração e global são publicados no CI.

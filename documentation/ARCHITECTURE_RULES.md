# Regras arquiteturais

ArchUnit analisa todo `com.example.nss` em `src/test/java/com/example/nss/architecture`.
As regras proíbem dependências entre `analytics` e `user`, dependências de
`shared` para módulos de negócio, acesso web a infraestrutura, e aplicação a
adapters concretos. Também verificam localização de controllers, repositories,
entidades JPA e configurações.

ArchUnit verifica estrutura, imports e anotações; não prova que uma regra de
negócio foi conceitualmente colocada no módulo certo. Essa parte é coberta por
testes unitários, integração PostgreSQL, contratos HTTP e revisão arquitetural.

## Estado da implementação

As regras de dependência entre módulos, domínio, web e infraestrutura estão
implementadas e os testes negativos demonstram rejeição de violações. Os
fluxos de primeiro acesso e refresh usam `FirstAccessGateway` e
`RefreshSessionGateway` na aplicação, com adapters JPA somente na
infraestrutura. A suíte arquitetural contém 16 testes verdes, incluindo três
fixtures negativas.

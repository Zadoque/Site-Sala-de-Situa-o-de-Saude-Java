# Testes e mutação

O PIT cobre application/domain de `analytics` e `user`, com
`failWhenNoMutations=true`, mutation score mínimo de 80% e test strength mínimo
de 80%. A execução atual gerou 215 mutantes e matou 187, mantendo score de 87%,
strength de 93% e 13 mutantes sem cobertura. Os sobreviventes estão classificados em
`PIT_CLASSIFICACAO_MUTACOES.md`.

O CI também executa relatórios separados: `analytics` gerou 33 mutantes e
matou 27 (82%, strength 96%); `user` gerou 182 e matou 160 (88%, strength
92%). Assim, o resultado de um módulo não mascara o outro.

JaCoCo mantém relatórios unitário, integração e global, com gates de 90% de
linhas, 75% de branches e 88% de métodos; a execução final passou esses gates.
O relatório global exclui somente adapters HTTP finos, DTOs, configuração
técnica, classes geradas e tipos estruturais sem regra de negócio; controllers,
contratos HTTP e configuração continuam cobertos por testes de contrato,
integração e ArchUnit.
Foram aprovados 128 testes unitários e 50 testes de integração. Testcontainers/
PostgreSQL é necessário para validar SQL produtivo; H2 é apenas apoio para
testes rápidos. Os dois testes concorrentes que carregam o contexto sem perfil
definem um secret JWT exclusivamente via `@TestPropertySource`; isso nunca é
fallback de produção.

# Testes e mutação

O PIT cobre application/domain de `analytics` e `user`, com
`failWhenNoMutations=true`, mutation score mínimo de 80% e test strength mínimo
de 80%. A execução anterior gerou 196 mutantes; após a proteção de snapshots
e do filtro UF, a última gerou 199 e matou 174, mantendo score de 87% e
strength de 92%. Os sobreviventes estão classificados em
`PIT_CLASSIFICACAO_MUTACOES.md`.

JaCoCo mantém relatórios unitário, integração e global, com gates de 90% de
linhas, 75% de branches e 88% de métodos; a execução final passou esses gates.
Foram aprovados 113 testes unitários e 48 testes de integração. Testcontainers/
PostgreSQL é necessário para validar SQL produtivo; H2 é apenas apoio para
testes rápidos. O segredo JWT de testes fica em
`src/test/resources/application-test.yml` e nunca é fallback de produção.

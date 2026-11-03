# Testes e mutação

O PIT cobre application/domain de `analytics` e `user`, com `failWhenNoMutations`
e thresholds de 80%. JaCoCo mantém relatórios unitário, integração e global.
Testcontainers/PostgreSQL é necessário para validar SQL produtivo; H2 é apenas
apoio para testes rápidos. O segredo JWT de testes fica em
`src/test/resources/application-test.yml` e nunca é fallback de produção.

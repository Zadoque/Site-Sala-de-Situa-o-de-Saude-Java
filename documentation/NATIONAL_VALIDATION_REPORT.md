# Relatório de validação nacional

A branch de referência da pipeline (`work/nss-pipeline`, `fbe7118`) foi
inspecionada. O contrato confirma identidade municipal IBGE de sete dígitos,
`dim_municipio`, `dim_doenca`, `fato_casos` e `pipeline_publications`.

A validação local usa fixtures PostgreSQL/H2 compatíveis com o contrato
inspecionado. A suíte final passou 48 testes de integração, incluindo
`EpidemiologyRepositoryPostgresIT`, com PostgreSQL 16 em Testcontainers. Ela
confirma SQL parametrizado, filtros, agregações e separação do datasource
operacional, mas não substitui uma execução contra o schema serving publicado.
Também há cenários PostgreSQL para seleção do snapshot mais recente por
município e para filtragem pelo código IBGE da UF.

O catálogo municipal e de doenças é resolvido dinamicamente por
`dim_municipio`, `dim_doenca` e `pipeline_publications`; portanto não há um
resultado honesto de quantidade nacional sem conexão ao banco publicado. A
homologação nacional em produção, latência com milhões de fatos, plano
`EXPLAIN ANALYZE` e a contagem efetiva de doenças publicadas permanecem
pendentes de acesso ao PostgreSQL serving configurado. Não são declaradas como
validadas por fixtures pequenas.

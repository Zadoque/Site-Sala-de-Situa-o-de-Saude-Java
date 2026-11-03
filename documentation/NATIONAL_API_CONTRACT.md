# Contrato nacional da API

`/api/v1/diseases`, `/metadata` e `/epidemiology/{region,state,municipalities,districts,neighborhoods}`
usam o mesmo filtro tipado e preservam `totalNotifications`, `coverage`, `items`
e `filters`. Municípios são resolvidos por `analytics.dim_municipio`, não por
lista no código Java. Regiões são derivadas da UF oficial publicada; o backend
não consulta o IBGE por requisição.

O filtro `ufCode`, quando informado, usa o código IBGE numérico de dois dígitos
presente em `analytics.dim_municipio.cd_uf` (por exemplo, `33` para RJ). A
combinação com `municipalityCode` é validada contra essa dimensão.

Doenças são anunciadas somente quando há publicação em
`analytics.pipeline_publications`. A dimensão pode reconhecer uma doença sem
que ela seja publicada; esses estados não são equivalentes.

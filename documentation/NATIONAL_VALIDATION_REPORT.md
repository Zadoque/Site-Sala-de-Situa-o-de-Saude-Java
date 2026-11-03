# Relatório de validação nacional

A branch de referência da pipeline (`work/nss-pipeline`, `fbe7118`) foi
inspecionada. O contrato confirma identidade municipal IBGE de sete dígitos,
`dim_municipio`, `dim_doenca`, `fato_casos` e `pipeline_publications`.

A validação local usa fixtures e o schema analítico real descrito pela pipeline.
Homologação nacional em produção, latência com milhões de fatos e contagem de
doenças publicadas dependem de acesso ao PostgreSQL serving configurado; não
foram declaradas como comprovadas sem esse ambiente.

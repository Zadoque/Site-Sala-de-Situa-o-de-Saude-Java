# Decisões arquiteturais

* O monólito modular preserva implantação e segurança existentes, isolando
  mudanças analíticas de autenticação.
* Ports foram criados para consultas, catálogo, autenticação, tokens e
  notificações, que são fronteiras substituíveis entre aplicação e detalhes
  técnicos.
* O schema `analytics` pertence à pipeline; migrations Flyway do Java ficam no
  banco operacional de usuários.
* A pipeline publica `fato_casos` por substituição transacional do recorte
  doença/ano/município. A consulta Java não reavalia o histórico de
  `pipeline_publications` com subconsultas correlacionadas: fatos presentes no
  serving são considerados a publicação corrente e o histórico continua sendo
  usado para disponibilidade e rastreabilidade.
* Uma futura tabela `analytics.pipeline_current_publication`, mantida pela
  pipeline e indexada por `(disease_codigo, ano, cd_mun)`, pode explicitar esse
  contrato caso o serving passe a conservar múltiplos lotes de fatos. Ela não
  deve ser criada por Flyway no backend Java.

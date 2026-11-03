# Decisões arquiteturais

* O monólito modular preserva implantação e segurança existentes, isolando
  mudanças analíticas de autenticação.
* Ports foram criados para consultas, catálogo, autenticação, tokens e
  notificações, que são fronteiras substituíveis entre aplicação e detalhes
  técnicos.
* O schema `analytics` pertence à pipeline; migrations Flyway do Java ficam no
  banco operacional de usuários.

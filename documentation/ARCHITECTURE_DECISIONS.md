# Decisões arquiteturais

* O monólito modular preserva implantação e segurança existentes, isolando
  mudanças analíticas de autenticação.
* Ports foram criados somente para consultas e catálogo, que são fronteiras
  substituíveis entre aplicação e PostgreSQL/arquivos.
* O schema `analytics` pertence à pipeline; migrations Flyway do Java ficam no
  banco operacional de usuários.

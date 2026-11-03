# Arquitetura do backend NSS

O NSS é um monólito modular Spring Boot 4 / Java 21. `analytics` e `user` são
bounded contexts na mesma aplicação; `shared` contém apenas mecanismos
transversais e `bootstrap` contém composição. Spring MVC permanece como
adaptador HTTP e PostgreSQL analítico é um datasource separado, somente leitura.

Casos de uso dependem de ports em `application.port`. JDBC, SQL, catálogos de
arquivo e configurações técnicas ficam em `infrastructure`. O domínio não
conhece Spring, HTTP ou persistência. A separação é pragmática: componentes de
aplicação podem usar `@Service`/`@Transactional` para integração com Spring,
sem importar adapters concretos.

Novas funcionalidades devem ser adicionadas no módulo responsável, expondo
contratos pequenos na aplicação quando houver uma fronteira real. Não há
microsserviços, CQRS ou mensageria nesta decisão.

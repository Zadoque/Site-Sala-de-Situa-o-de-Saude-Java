package com.example.nss.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.nss", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

  @ArchTest
  static final ArchRule analytics_must_not_depend_on_user =
      noClasses()
          .that()
          .resideInAnyPackage("..analytics..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..user..");

  @ArchTest
  static final ArchRule user_must_not_depend_on_analytics =
      noClasses()
          .that()
          .resideInAnyPackage("..user..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..analytics..");

  @ArchTest
  static final ArchRule controllers_must_not_access_infrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("..web.controller..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..infrastructure..");

  @ArchTest
  static final ArchRule applications_must_not_access_web =
      noClasses()
          .that()
          .resideInAnyPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..web..");

  @ArchTest
  static final ArchRule applications_must_not_access_infrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..infrastructure..");

  @ArchTest
  static final ArchRule repositories_must_not_access_web =
      noClasses()
          .that()
          .resideInAnyPackage("..infrastructure.repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..web..");

  @ArchTest
  static final ArchRule domains_must_not_access_outer_layers =
      noClasses()
          .that()
          .resideInAnyPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..web..", "..api..", "..application..", "..infrastructure..", "..security..");

  @ArchTest
  static final ArchRule web_must_not_access_infrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("..web.controller..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..infrastructure..");

  @ArchTest
  static final ArchRule domain_must_be_technology_free =
      noClasses()
          .that()
          .resideInAnyPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "org.hibernate..",
              "org.springframework.jdbc..",
              "jakarta.servlet..",
              "..infrastructure..");

  @ArchTest
  static final ArchRule persistence_entities_are_in_infrastructure =
      classes()
          .that()
          .areAnnotatedWith(jakarta.persistence.Entity.class)
          .should()
          .resideInAnyPackage("..infrastructure.persistence..");

  @ArchTest
  static final ArchRule controllers_must_not_access_repositories =
      noClasses()
          .that()
          .resideInAnyPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..repository..");

  @ArchTest
  static final ArchRule repositories_must_not_access_services =
      noClasses()
          .that()
          .resideInAnyPackage("..repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..service..");

  @ArchTest
  static final ArchRule applications_must_not_access_controllers =
      noClasses()
          .that()
          .resideInAnyPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..");
}

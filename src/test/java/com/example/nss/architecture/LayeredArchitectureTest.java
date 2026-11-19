package com.example.nss.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

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
  static final ArchRule services_must_not_access_http_dtos =
      noClasses()
          .that()
          .resideInAnyPackage("..service..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..dto.request..", "..dto.response..", "..dto.mapper..");

  @ArchTest
  static final ArchRule entities_must_not_access_outer_layers =
      noClasses()
          .that()
          .resideInAnyPackage("..entity..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..", "..service..", "..repository..", "..dto..");

  @ArchTest
  static final ArchRule repositories_are_located_in_repository =
      classes()
          .that()
          .areAnnotatedWith(Repository.class)
          .should()
          .resideInAnyPackage("..repository..");

  @ArchTest
  static final ArchRule rest_controllers_are_located_in_controller =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .resideInAnyPackage("..controller..");

  @ArchTest
  static final ArchRule configurations_are_located_in_config_or_security =
      classes()
          .that()
          .areAnnotatedWith(Configuration.class)
          .should()
          .resideInAnyPackage("..config..")
          .orShould()
          .haveSimpleName("SecurityConfig");

  @ArchTest
  static final ArchRule services_are_located_in_service =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .and()
          .resideOutsideOfPackage("..user.security..")
          .should()
          .resideInAnyPackage("..service..");

  @ArchTest
  static final ArchRule security_services_are_explicit =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .and()
          .resideInAnyPackage("..user.security..")
          .should()
          .haveSimpleName("JwtService")
          .orShould()
          .haveSimpleName("CustomUserDetailsService");

  @ArchTest
  static final ArchRule old_packages_are_absent =
      noClasses()
          .that()
          .resideInAnyPackage(
              "..api..", "..application..", "..domain..", "..infrastructure..", "..web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..api..", "..application..", "..domain..", "..infrastructure..", "..web..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule no_classes_remain_in_old_packages =
      classes()
          .that()
          .resideInAnyPackage(
              "..api..", "..application..", "..domain..", "..infrastructure..", "..web..")
          .should()
          .haveSimpleName("__no_classes_in_legacy_packages__")
          .allowEmptyShould(true);
}

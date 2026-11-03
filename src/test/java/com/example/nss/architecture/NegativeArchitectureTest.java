package com.example.nss.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class NegativeArchitectureTest {
  private final JavaClasses fixtures =
      new ClassFileImporter()
          .importPackages("com.example.architecture.fixtures", "com.example.nss");

  @Test
  void controllerRepositoryViolationIsDetected() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleName("IllegalController")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..");
    assertThrows(AssertionError.class, () -> rule.check(fixtures));
  }

  @Test
  void applicationHttpViolationIsDetected() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleName("IllegalApplication")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..web..");
    assertThrows(AssertionError.class, () -> rule.check(fixtures));
  }

  @Test
  void domainTechnologyViolationIsDetected() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleName("IllegalDomain")
            .should()
            .beAnnotatedWith(jakarta.persistence.Entity.class);
    assertThrows(AssertionError.class, () -> rule.check(fixtures));
  }
}

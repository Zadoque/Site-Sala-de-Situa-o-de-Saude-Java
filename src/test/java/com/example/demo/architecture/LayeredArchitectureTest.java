package com.example.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.demo")
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories = noClasses()
            .that().resideInAnyPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");

    @ArchTest
    static final ArchRule repositories_must_not_access_services = noClasses()
            .that().resideInAnyPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..");

    @ArchTest
    static final ArchRule services_must_not_access_controllers = noClasses()
            .that().resideInAnyPackage("..service..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..");
}

package com.minimarket.pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.minimarket.pos", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularArchitectureTest {

    @ArchTest
    static final ArchRule modulesMustBeFreeOfCycles = slices()
            .matching("com.minimarket.pos.(*)..")
            .should()
            .beFreeOfCycles();

    @ArchTest
    static final ArchRule domainMustNotDependOnDeliveryOrInfrastructure = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..api..", "..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule modulesMustNotUseAnotherModulesInternals = noClasses()
            .that()
            .resideOutsideOfPackage("com.minimarket.pos.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.minimarket.pos.identity.infrastructure..",
                    "com.minimarket.pos.catalog.infrastructure..",
                    "com.minimarket.pos.inventory.infrastructure..",
                    "com.minimarket.pos.cash.infrastructure..",
                    "com.minimarket.pos.sales.infrastructure..",
                    "com.minimarket.pos.scanner.infrastructure..",
                    "com.minimarket.pos.reporting.infrastructure..")
            .allowEmptyShould(true);
}

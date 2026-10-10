package dev.maxenceng.lazyclaude;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import dev.maxenceng.lazyclaude.error.domain.DomainException;

/*
 * The architecture, as rules that fail the build. Each direct subpackage of
 * dev.maxenceng.lazyclaude is a module whose package-info declares its kind with
 * @ApplicationModule: a context, shaped as {published, domain, application,
 * infrastructure.{primary, secondary}}; a kernel (`shared`, `error`); or the shell (the TUI).
 * The rules between modules read that kind rather than a list of package names.
 *
 * Rules over packages that do not exist yet allow an empty match: the template ships no
 * context, and a rule must not fail on the absence of the thing it governs.
 */
@AnalyzeClasses(packages = "dev.maxenceng.lazyclaude", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "dev.maxenceng.lazyclaude";

    // The domain is plain Java: it must be testable without a terminal, a process or a file.
    @ArchTest
    static final ArchRule domain_depends_on_nothing_but_the_jdk_and_other_domains = classes()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "..domain..", "..published..")
            .allowEmptyShould(true);

    // Dependencies point inward: infrastructure -> application -> domain.
    @ArchTest
    static final ArchRule application_does_not_reach_into_infrastructure = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    // A secondary adapter implements a port; calling back into the application inverts the arrows.
    @ArchTest
    static final ArchRule secondary_adapters_do_not_call_the_application = noClasses()
            .that()
            .resideInAPackage("..infrastructure.secondary..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..application..")
            .allowEmptyShould(true);

    // TamboUI and JLine are the screen; only the shell and primary adapters may draw.
    @ArchTest
    static final ArchRule only_the_shell_and_primary_adapters_touch_the_terminal = noClasses()
            .that()
            .resideOutsideOfPackages(ROOT + ".shell..", "..infrastructure.primary..")
            .and()
            .doNotHaveFullyQualifiedName(LazyClaude.class.getName())
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("dev.tamboui..", "org.jline..")
            .allowEmptyShould(true);

    // Processes, files, watches and the network are side effects; they live behind a port, in a secondary adapter.
    @ArchTest
    static final ArchRule side_effects_live_in_secondary_adapters = noClasses()
            .that()
            .resideOutsideOfPackage("..infrastructure.secondary..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(ProcessBuilder.class.getName())
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName(Process.class.getName())
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName(java.nio.file.Files.class.getName())
            .orShould()
            .dependOnClassesThat()
            .resideInAPackage("java.net.http..")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName(java.nio.file.WatchService.class.getName())
            .allowEmptyShould(true);

    // A module's kind decides what the other rules allow it, so an undeclared one would escape them all.
    @ArchTest
    static final ArchRule every_module_declares_its_kind = classes()
            .that()
            .resideInAPackage(ROOT + ".*..")
            .should(beInAModuleThatDeclaresItsKind())
            .allowEmptyShould(true);

    // Contexts are reached through their `published` package, never by importing their insides.
    @ArchTest
    static final ArchRule contexts_meet_only_through_published = classes()
            .that()
            .resideInAPackage(ROOT + ".*..")
            .should(onlyReachOtherContextsThroughPublished())
            .allowEmptyShould(true);

    // The shell asks a context through its primary adapter and reads what that hands back; it never sees a domain.
    @ArchTest
    static final ArchRule the_shell_reaches_contexts_only_through_primary_and_published = classes()
            .that()
            .resideInAPackage(ROOT + ".*..")
            .should(reachContextsOnlyThroughPrimaryAndPublishedFromTheShell())
            .allowEmptyShould(true);

    // The shell composes contexts; a context that knew about the shell could not be reused or tested alone.
    @ArchTest
    static final ArchRule nothing_depends_on_the_shell = classes()
            .that()
            .resideInAPackage(ROOT + "..")
            .and()
            .doNotHaveFullyQualifiedName(LazyClaude.class.getName())
            .should(notReachTheShellFromOutsideIt())
            .allowEmptyShould(true);

    // One type per file keeps each concept findable by name; a builder is part of the type it builds.
    @ArchTest
    static final ArchRule no_nested_types_but_builders = classes()
            .that()
            .resideInAPackage(ROOT + "..")
            .and()
            .areNestedClasses()
            .and()
            .areNotAnonymousClasses()
            .should()
            .haveSimpleNameEndingWith("Builder")
            .allowEmptyShould(true);

    // An aggregate hands back the list it was built from; a copy in one type reads as a guarantee the others do not give.
    @ArchTest
    static final ArchRule domain_classes_do_not_copy_collections_defensively = noClasses()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .resideOutsideOfPackage(ROOT + ".error..")
            .should()
            .callMethodWhere(target(name("copyOf")).and(target(owner(nameMatching("java\\.util\\.(List|Set|Map)")))))
            .allowEmptyShould(true);

    // A raw value belongs in the value object that names it, not spread across the types that use it.
    @ArchTest
    static final ArchRule composite_domain_types_hold_value_objects = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .resideOutsideOfPackage(ROOT + ".error..")
            .and()
            .areNotEnums()
            .and()
            .areNotAssignableTo(Throwable.class)
            .and()
            .haveSimpleNameNotEndingWith("Builder")
            .should(holdOnlyDomainTypesWhenTheyHoldMoreThanOne())
            .allowEmptyShould(true);

    // With two fields or more, a builder names each argument; a reordered constructor call would compile and lie.
    @ArchTest
    static final ArchRule composite_domain_types_are_built_through_their_own_builder = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .resideOutsideOfPackage(ROOT + ".error..")
            .and()
            .areNotEnums()
            .and()
            .areNotAssignableTo(Throwable.class)
            .and()
            .haveSimpleNameNotEndingWith("Builder")
            .and()
            .haveSimpleNameNotEndingWith("Manager")
            .should(beBuiltOnlyByTheirOwnBuilderWhenTheyHoldMoreThanOne())
            .allowEmptyShould(true);

    // A view is a read model as wide as the domain type it shows, so it is built the same way.
    @ArchTest
    static final ArchRule views_are_built_through_their_own_builder = classes()
            .that()
            .resideInAPackage("..infrastructure.primary..")
            .and()
            .haveSimpleNameEndingWith("View")
            .should(beBuiltOnlyByTheirOwnBuilderEvenFromWithin())
            .allowEmptyShould(true);

    // The start-up reports a DomainException's message; any other exception escapes as a stack trace.
    @ArchTest
    static final ArchRule business_failures_extend_domain_exception = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .resideOutsideOfPackage(ROOT + ".error..")
            .and()
            .haveSimpleNameEndingWith("Exception")
            .should()
            .beAssignableTo(DomainException.class)
            .allowEmptyShould(true);

    // A domain object that can change after construction cannot hold an invariant.
    @ArchTest
    static final ArchRule domain_state_is_final = fields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .areDeclaredInClassesThat()
            .haveSimpleNameNotEndingWith("Builder")
            .should()
            .beFinal()
            .allowEmptyShould(true);

    // Domain types say what they do; a setter says only that a field changes.
    @ArchTest
    static final ArchRule domain_types_expose_no_setters = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage("..domain..", "..published..")
            .should()
            .haveNameMatching("set[A-Z].*")
            .allowEmptyShould(true);

    // A manager orders its rules and raises its own context's exceptions; asserting is second-guessing what it was handed.
    @ArchTest
    static final ArchRule managers_assert_nothing = noClasses()
            .that()
            .haveSimpleNameEndingWith("Manager")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(ROOT + ".error..")
            .allowEmptyShould(true);

    // A port is held by the domain service that needs it, not by an adapter or a use case.
    @ArchTest
    static final ArchRule ports_are_held_only_by_the_domain = fields()
            .that()
            .haveRawType(nameMatching(".*Port"))
            .should()
            .beDeclaredInClassesThat()
            .resideInAPackage("..domain..")
            .allowEmptyShould(true);

    // A manager wired into another is a second place deciding what that one's use case means.
    @ArchTest
    static final ArchRule managers_are_wired_with_ports_not_other_managers = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .should(takeNoOtherManagerAsAConstructorParameter())
            .allowEmptyShould(true);

    // A list, a map and a pair are shapes rather than values; what they carry must still be a domain type.
    private static final List<String> CARRIERS = List.of(List.class.getName(), Map.class.getName());
    private static final String PAIR = "Pair";

    private static List<JavaField> nonStaticFields(JavaClass type) {
        return type.getAllFields().stream()
                .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                .toList();
    }

    private static ArchCondition<JavaClass> holdOnlyDomainTypesWhenTheyHoldMoreThanOne() {
        return new ArchCondition<>("hold only domain types when they hold more than one") {
            @Override
            public void check(JavaClass owner, ConditionEvents events) {
                List<JavaField> components = nonStaticFields(owner);
                if (components.size() < 2) {
                    return;
                }
                for (JavaField component : components) {
                    JavaClass type = component.getRawType();
                    if (CARRIERS.contains(type.getName()) || type.getSimpleName().equals(PAIR)) {
                        carriedTypes(component)
                                .filter(ArchitectureTest::isRawValue)
                                .forEach(carried -> events.add(SimpleConditionEvent.violated(
                                        component,
                                        "%s holds %s<%s>; the carrier is fine, what it carries is a raw value"
                                                .formatted(owner.getSimpleName(), type.getSimpleName(), carried.getSimpleName()))));
                    } else if (isRawValue(type)) {
                        events.add(SimpleConditionEvent.violated(
                                component,
                                "%s holds %s directly; wrap it in a value object"
                                        .formatted(owner.getSimpleName(), type.getSimpleName())));
                    }
                }
            }
        };
    }

    private static Stream<JavaClass> carriedTypes(JavaField component) {
        return component.getType() instanceof JavaParameterizedType parameterized
                ? parameterized.getActualTypeArguments().stream().map(JavaType::toErasure)
                : Stream.empty();
    }

    private static boolean isRawValue(JavaClass type) {
        if (type.getName().equals(boolean.class.getName())) {
            return false;
        }
        return type.isPrimitive() || type.getPackageName().startsWith("java.");
    }

    private static ArchCondition<JavaClass> beBuiltOnlyByTheirOwnBuilderWhenTheyHoldMoreThanOne() {
        return new ArchCondition<>("be built only by their own builder when they hold more than one field") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (nonStaticFields(type).size() < 2) {
                    return;
                }
                Stream.<JavaCodeUnitAccess<?>>concat(
                                type.getConstructorCallsToSelf().stream(), type.getConstructorReferencesToSelf().stream())
                        .filter(construction -> !isBuiltThroughItsBuilder(type, construction.getOriginOwner()))
                        .forEach(construction -> events.add(SimpleConditionEvent.violated(
                                construction,
                                "%s constructs %s directly; build it through %sBuilder"
                                        .formatted(
                                                construction.getOriginOwner().getSimpleName(),
                                                type.getSimpleName(),
                                                type.getSimpleName()))));
            }
        };
    }

    private static ArchCondition<JavaClass> beBuiltOnlyByTheirOwnBuilderEvenFromWithin() {
        return new ArchCondition<>("be built only by their own builder, even from their own factory") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (nonStaticFields(type).size() < 2) {
                    return;
                }
                Stream.<JavaCodeUnitAccess<?>>concat(
                                type.getConstructorCallsToSelf().stream(), type.getConstructorReferencesToSelf().stream())
                        .filter(construction -> !isOwnBuilder(type, construction.getOriginOwner()))
                        .forEach(construction -> events.add(SimpleConditionEvent.violated(
                                construction,
                                "%s constructs %s directly; build it through %sBuilder"
                                        .formatted(
                                                construction.getOriginOwner().getSimpleName(),
                                                type.getSimpleName(),
                                                type.getSimpleName()))));
            }
        };
    }

    private static boolean isOwnBuilder(JavaClass type, JavaClass origin) {
        return origin.getSimpleName().equals(type.getSimpleName() + "Builder")
                && origin.getEnclosingClass().filter(type::equals).isPresent();
    }

    private static boolean isBuiltThroughItsBuilder(JavaClass type, JavaClass origin) {
        return origin.equals(type) || isOwnBuilder(type, origin);
    }

    private static ArchCondition<JavaClass> takeNoOtherManagerAsAConstructorParameter() {
        return new ArchCondition<>("take no other manager as a constructor parameter") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                type.getConstructors()
                        .forEach(wiring -> wiring.getRawParameterTypes().stream()
                                .filter(collaborator -> collaborator.getSimpleName().endsWith("Manager"))
                                .filter(collaborator -> !collaborator.equals(type))
                                .forEach(collaborator -> events.add(SimpleConditionEvent.violated(
                                        wiring,
                                        "%s is wired with %s; a manager depends on ports only"
                                                .formatted(type.getSimpleName(), collaborator.getSimpleName())))));
            }
        };
    }

    private static ArchCondition<JavaClass> beInAModuleThatDeclaresItsKind() {
        return new ArchCondition<>("be in a module whose package-info declares its kind") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                if (moduleOf(origin).isEmpty() && !origin.getPackageName().equals(ROOT)) {
                    events.add(SimpleConditionEvent.violated(origin, "%s is in %s, whose package-info has no @%s"
                            .formatted(origin.getName(), modulePackageName(origin), ApplicationModule.class.getSimpleName())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notReachTheShellFromOutsideIt() {
        return new ArchCondition<>("not depend on the shell from outside it") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                if (kindOf(origin) == ModuleType.SHELL) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    if (kindOf(dependency.getTargetClass()) == ModuleType.SHELL) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> onlyReachOtherContextsThroughPublished() {
        return new ArchCondition<>("only reach another context through its published package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String from = contextOf(origin);
                if (from == null) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String to = contextOf(target);
                    boolean foreign = to != null && !to.equals(from);
                    if (foreign && !isIn(target, to, "published")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> reachContextsOnlyThroughPrimaryAndPublishedFromTheShell() {
        return new ArchCondition<>("reach a context, from the shell, only through its infrastructure.primary or published package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                if (kindOf(origin) != ModuleType.SHELL) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String to = contextOf(target);
                    if (to != null && !isIn(target, to, "infrastructure.primary") && !isIn(target, to, "published")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static boolean isIn(JavaClass javaClass, String context, String layer) {
        String layerPackage = ROOT + "." + context + "." + layer;
        String name = javaClass.getPackageName();
        return name.equals(layerPackage) || name.startsWith(layerPackage + ".");
    }

    private static String contextOf(JavaClass javaClass) {
        return kindOf(javaClass) == ModuleType.CONTEXT ? moduleNameOf(javaClass) : null;
    }

    private static ModuleType kindOf(JavaClass javaClass) {
        return moduleOf(javaClass)
                .flatMap(module -> module.tryGetAnnotationOfType(ApplicationModule.class))
                .map(ApplicationModule::type)
                .orElse(null);
    }

    private static Optional<JavaPackage> moduleOf(JavaClass javaClass) {
        String module = modulePackageName(javaClass);
        if (module == null) {
            return Optional.empty();
        }
        JavaPackage candidate = javaClass.getPackage();
        while (!candidate.getName().equals(module)) {
            candidate = candidate.getParent().orElseThrow();
        }
        return candidate.isAnnotatedWith(ApplicationModule.class) ? Optional.of(candidate) : Optional.empty();
    }

    private static String moduleNameOf(JavaClass javaClass) {
        String module = modulePackageName(javaClass);
        return module == null ? null : module.substring(ROOT.length() + 1);
    }

    private static String modulePackageName(JavaClass javaClass) {
        String name = javaClass.getPackageName();
        if (!name.startsWith(ROOT + ".")) {
            return null;
        }
        return ROOT + "." + name.substring(ROOT.length() + 1).split("\\.")[0];
    }
}

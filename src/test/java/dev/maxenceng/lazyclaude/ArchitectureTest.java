package dev.maxenceng.lazyclaude;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
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

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.error.domain.AssertionException;
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
    private static final String KEY_EVENT = "dev.tamboui.tui.event.KeyEvent";
    // The composition root is main and the launches it picks from: the one place that prints and reaches every module (ADR 0077).
    private static final DescribedPredicate<JavaClass> THE_COMPOSITION_ROOT =
            JavaClass.Predicates.assignableTo(Launch.class).or(name(LazyClaude.class.getName()));

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
            .and(not(THE_COMPOSITION_ROOT))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("dev.tamboui..", "org.jline..")
            .allowEmptyShould(true);

    // A hosted session's pty is the secondary adapter's, and its emulator the TUI adapter's; each library stays on its side (ADR 0049).
    @ArchTest
    static final ArchRule pty4j_stays_in_the_session_secondary_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(ROOT + ".session.infrastructure.secondary..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.pty4j..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule jediterm_stays_in_the_session_tui_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(ROOT + ".session.infrastructure.primary..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.jediterm..")
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

    // A line printed while the screen is up lands on the drawn frame (ADR 0022); only the refusal to start prints.
    @ArchTest
    static final ArchRule only_the_composition_root_prints = noClasses()
            .that(not(THE_COMPOSITION_ROOT))
            .should()
            .accessField(System.class, "err")
            .orShould()
            .accessField(System.class, "out")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName(java.io.PrintStream.class.getName())
            .orShould()
            .callMethod(Throwable.class, "printStackTrace")
            .allowEmptyShould(true);

    // A task without an executor runs on the common pool, where a blocking read starves every other user of it.
    @ArchTest
    static final ArchRule async_work_names_its_executor = noClasses()
            .should()
            .callMethod(CompletableFuture.class, "supplyAsync", Supplier.class)
            .orShould()
            .callMethod(CompletableFuture.class, "runAsync", Runnable.class)
            .allowEmptyShould(true);

    // Every context's Query and Command is built once, by the composition root, and handed whole to the shell or the sidecar (ADR 0077).
    @ArchTest
    static final ArchRule only_the_composition_root_builds_a_query_or_a_command = noClasses()
            .that()
            .resideInAPackage(ROOT + ".*..")
            .should()
            .callConstructorWhere(target(owner(resideInAPackage("..infrastructure.primary..")
                    .and(nameMatching(".*\\.[A-Za-z]+(Query|Command)")))))
            .allowEmptyShould(true);

    // A key is only ever read from the terminal; a click that acts as a key calls the operation that key calls (ADR 0051).
    @ArchTest
    static final ArchRule a_key_is_never_replayed = noClasses()
            .should()
            .callMethodWhere(target(owner(name(KEY_EVENT))).and(target(nameMatching("of(Key|Char)"))))
            .orShould()
            .callConstructorWhere(target(owner(name(KEY_EVENT))))
            .allowEmptyShould(true);

    // An Optional is unwrapped where it is checked, by map, or, orElse or a named exception; get() and orElseThrow() with no argument do not say what was absent.
    @ArchTest
    static final ArchRule an_optional_is_never_unwrapped_unchecked = noClasses()
            .should()
            .callMethod(Optional.class, "get")
            .orShould()
            .callMethod(Optional.class, "orElseThrow")
            .allowEmptyShould(true);

    // Jackson parses what other tools write; it stays in the adapter that reads it and in the sidecar that speaks JSON (ADR 0050), never in a domain or a screen.
    @ArchTest
    static final ArchRule json_is_parsed_only_in_secondary_adapters_and_the_sidecar = noClasses()
            .that()
            .resideOutsideOfPackages("..infrastructure.secondary..", ROOT + ".sidecar..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("tools.jackson..", "com.fasterxml.jackson..")
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

    // `published` is what another module reads: a type there that nothing outside reads was built ahead of its reader.
    @ArchTest
    static final ArchRule published_types_are_read_outside_their_module = classes()
            .that()
            .resideInAPackage("..published..")
            .should(beReadFromOutsideTheirModule())
            .allowEmptyShould(true);

    // The shell asks a context or a kernel through its primary adapter and reads what that hands back; it never sees a domain (ADR 0022).
    @ArchTest
    static final ArchRule the_shell_reaches_modules_only_through_primary_and_published = classes()
            .that()
            .resideInAPackage(ROOT + ".*..")
            .should(reachModulesOnlyThroughPrimaryAndPublishedFromTheShell())
            .allowEmptyShould(true);

    // The shell and the sidecar ask a primary adapter in its roles only: a Query reads, a Command writes, a Payload goes in, a View comes out (ADR 0012, ADR 0049).
    @ArchTest
    static final ArchRule the_shell_reads_primary_adapters_only_through_their_roles = noClasses()
            .that()
            .resideInAnyPackage(ROOT + ".shell..", ROOT + ".sidecar..")
            .should()
            .dependOnClassesThat(resideInAPackage("..infrastructure.primary..")
                    .and(not(nameMatching(".*(Query|Command|Payload|View)(\\$[A-Za-z]+Builder)?"))))
            .allowEmptyShould(true);

    // A shell (the TUI, the sidecar) composes contexts; a context that knew about one could not be reused or tested alone, and a shell never leans on the other.
    @ArchTest
    static final ArchRule nothing_depends_on_the_shell = classes()
            .that()
            .resideInAPackage(ROOT + "..")
            .and(not(THE_COMPOSITION_ROOT))
            .should(notReachTheShellFromOutsideIt())
            .allowEmptyShould(true);

    // A type's name carries its context: a bare Config, Value or Entry prefix leaves the reader to guess which file or which value.
    @ArchTest
    static final ArchRule type_names_carry_their_context = noClasses()
            .that()
            .resideInAPackage(ROOT + "..")
            .should()
            .haveNameMatching(".*\\.(Config|Value|Entry)([A-Z][^.]*)?")
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

    // A type hands back the collection it was given; a copy in one type reads as a guarantee the others do not give (ADR 0075).
    @ArchTest
    static final ArchRule no_class_copies_a_collection = noClasses()
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
            .should(holdOnlyDomainTypes())
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

    // A composite is assembled by whoever reads its parts, and a sealed type is a choice its reader makes; a static factory on
    // either moves that reading into the type and passes the builder rule through a call to self (ADR 0060).
    @ArchTest
    static final ArchRule composite_and_sealed_types_declare_no_static_factory = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .resideOutsideOfPackage(ROOT + ".error..")
            .and()
            .areNotEnums()
            .and()
            .haveSimpleNameNotEndingWith("Builder")
            .should(buildNothingOfThemselvesFromAStaticMethodWhenCompositeOrSealed())
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

    // A failure is named for what went wrong; a bare JDK exception says only where it was thrown.
    @ArchTest
    static final ArchRule failures_are_named_exceptions = noClasses()
            .that()
            .areNotAssignableTo(Throwable.class)
            .should()
            .callConstructor(java.io.UncheckedIOException.class, java.io.IOException.class)
            .orShould()
            .callConstructor(java.io.UncheckedIOException.class, String.class, java.io.IOException.class)
            .orShould()
            .callConstructorWhere(target(owner(nameMatching("java\\.lang\\.(Runtime|IllegalState|IllegalArgument)Exception"))))
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

    // A constant another class reads is a dependency nobody can see in a constructor; it is passed in, or kept in the package.
    @ArchTest
    static final ArchRule no_public_static_fields = fields()
            .that()
            .areDeclaredInClassesThat()
            .areNotEnums()
            .and()
            .areStatic()
            .should()
            .notBePublic()
            .allowEmptyShould(true);

    // A constructor only checks what it is handed, through Assert; a business rule that can fail belongs to the manager.
    @ArchTest
    static final ArchRule domain_constructors_throw_nothing_of_their_own = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .and()
            .resideOutsideOfPackage(ROOT + ".error..")
            .and()
            .areNotAssignableTo(Throwable.class)
            .should(throwNothingFromTheirConstructors())
            .allowEmptyShould(true);

    // A view answers the shell in the context's own types; a raw string parameter is a value object the caller skipped.
    @ArchTest
    static final ArchRule views_take_domain_types_as_parameters = classes()
            .that()
            .resideInAPackage("..infrastructure.primary..")
            .and()
            .haveSimpleNameEndingWith("View")
            .should(takeNoRawValueInAPublicMethod())
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

    // An adapter checks what a tool wrote before building from it; the domain is never asked to refuse it on the adapter's behalf (ADR 0043).
    @ArchTest
    static final ArchRule secondary_adapters_catch_no_domain_refusal = classes()
            .that()
            .resideInAPackage("..infrastructure.secondary..")
            .should(catchNoDomainRefusal())
            .allowEmptyShould(true);

    // Assert guards the domain's inputs; an adapter reading a gap through it is relying on the domain to find the gap (ADR 0043).
    @ArchTest
    static final ArchRule secondary_adapters_assert_nothing = noClasses()
            .that()
            .resideInAPackage("..infrastructure.secondary..")
            .should()
            .callMethodWhere(target(owner(name(Assert.class.getName()))))
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

    // A repository is a port's implementation (ADR 0011); a helper an adapter keeps for itself takes another name.
    @ArchTest
    static final ArchRule repositories_implement_a_port = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .implement(nameMatching(".*Port"))
            .allowEmptyShould(true);

    // A manager wired into another is a second place deciding what that one's use case means.
    @ArchTest
    static final ArchRule managers_are_wired_with_ports_not_other_managers = classes()
            .that()
            .resideInAnyPackage("..domain..", "..published..")
            .should(takeNoOtherManagerAsAConstructorParameter())
            .allowEmptyShould(true);

    private static final String MANAGER = "Manager";
    private static final String PORT = "Port";

    // A manager holds ports and the clock, nothing it did not compute itself: another manager's decision, even behind a
    // callback, is composed by the application service that calls both (ADR 0027).
    @ArchTest
    static final ArchRule managers_take_only_ports_and_the_clock = classes()
            .that()
            .resideInAPackage("..domain..")
            .and()
            .haveSimpleNameEndingWith(MANAGER)
            .should(takeOnlyPortsAndTheClockAsConstructorParameters())
            .allowEmptyShould(true);

    // A list, a map and a pair are shapes rather than values; what they carry must still be a domain type.
    private static final List<String> CARRIERS = List.of(List.class.getName(), Map.class.getName());
    private static final String PAIR = "Pair";
    private static final String EQUALS = "equals";

    private static List<JavaField> nonStaticFields(JavaClass type) {
        return type.getAllFields().stream()
                .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                .toList();
    }

    private static ArchCondition<JavaClass> holdOnlyDomainTypes() {
        return new ArchCondition<>("carry only domain types, and hold a raw value only as their single field") {
            @Override
            public void check(JavaClass owner, ConditionEvents events) {
                List<JavaField> components = nonStaticFields(owner);
                for (JavaField component : components) {
                    JavaClass type = component.getRawType();
                    if (CARRIERS.contains(type.getName()) || type.getSimpleName().equals(PAIR)) {
                        carriedTypes(component)
                                .filter(ArchitectureTest::isRawValue)
                                .forEach(carried -> events.add(SimpleConditionEvent.violated(
                                        component,
                                        "%s holds %s<%s>; the carrier is fine, what it carries is a raw value"
                                                .formatted(owner.getSimpleName(), type.getSimpleName(), carried.getSimpleName()))));
                    } else if (components.size() > 1 && isRawValue(type)) {
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

    private static ArchCondition<JavaClass> buildNothingOfThemselvesFromAStaticMethodWhenCompositeOrSealed() {
        return new ArchCondition<>("build nothing of themselves from a static method when composite or sealed") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (nonStaticFields(type).size() < 2 && !type.reflect().isSealed()) {
                    return;
                }
                Stream.<JavaCodeUnitAccess<?>>concat(
                                type.getConstructorCallsFromSelf().stream(), type.getConstructorReferencesFromSelf().stream())
                        .filter(construction -> construction.getOrigin().getModifiers().contains(JavaModifier.STATIC))
                        .filter(construction -> construction.getTargetOwner().isAssignableTo(type.getName()))
                        .forEach(construction -> events.add(SimpleConditionEvent.violated(
                                construction,
                                "%s builds %s in a static method; the reader of its parts builds it"
                                        .formatted(
                                                type.getSimpleName(),
                                                construction.getTargetOwner().getSimpleName()))));
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

    private static ArchCondition<JavaClass> throwNothingFromTheirConstructors() {
        return new ArchCondition<>("throw nothing of their own from a constructor") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                type.getConstructors().stream()
                        .flatMap(constructor -> constructor.getConstructorCallsFromSelf().stream())
                        .filter(call -> call.getTargetOwner().isAssignableTo(Throwable.class))
                        .forEach(call -> events.add(SimpleConditionEvent.violated(
                                call,
                                "%s's constructor throws %s; check inputs through Assert and leave business rules to the manager"
                                        .formatted(type.getSimpleName(), call.getTargetOwner().getSimpleName()))));
            }
        };
    }

    // An adapter's own unavailable exception may be caught to fall back; the catch-all and an assertion are the domain refusing.
    private static boolean isADomainRefusal(JavaClass caught) {
        return caught.isEquivalentTo(DomainException.class) || caught.isAssignableTo(AssertionException.class);
    }

    private static ArchCondition<JavaClass> catchNoDomainRefusal() {
        return new ArchCondition<>("catch neither DomainException itself nor an assertion's refusal") {
            @Override
            public void check(JavaClass adapter, ConditionEvents events) {
                adapter.getCodeUnits().stream()
                        .flatMap(unit -> unit.getTryCatchBlocks().stream())
                        .filter(block -> block.getCaughtThrowables().stream().anyMatch(ArchitectureTest::isADomainRefusal))
                        .forEach(block -> events.add(SimpleConditionEvent.violated(
                                block,
                                "%s catches a domain refusal at %s; check the record for what it lacks before building from it"
                                        .formatted(adapter.getSimpleName(), block.getSourceCodeLocation()))));
            }
        };
    }

    private static ArchCondition<JavaClass> takeNoRawValueInAPublicMethod() {
        return new ArchCondition<>("take no raw value as a parameter of a public method") {
            @Override
            public void check(JavaClass view, ConditionEvents events) {
                view.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(method -> !method.getName().equals(EQUALS))
                        .forEach(method -> method.getRawParameterTypes().stream()
                                .filter(ArchitectureTest::isRawValue)
                                .forEach(raw -> events.add(SimpleConditionEvent.violated(
                                        method,
                                        "%s takes %s; ask in the domain's type for it"
                                                .formatted(method.getFullName(), raw.getSimpleName())))));
            }
        };
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

    private static ArchCondition<JavaClass> takeOnlyPortsAndTheClockAsConstructorParameters() {
        return new ArchCondition<>("take only ports and the clock as constructor parameters") {
            @Override
            public void check(JavaClass manager, ConditionEvents events) {
                manager.getConstructors()
                        .forEach(wiring -> wiring.getRawParameterTypes().stream()
                                .filter(collaborator -> !collaborator.getSimpleName().endsWith(PORT))
                                .filter(collaborator -> !collaborator.isEquivalentTo(Clock.class))
                                .forEach(collaborator -> events.add(SimpleConditionEvent.violated(
                                        wiring,
                                        "%s is wired with %s; a manager holds ports, and another manager's answer is the application service's to pass on"
                                                .formatted(manager.getSimpleName(), collaborator.getSimpleName())))));
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
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean otherShell = kindOf(target) == ModuleType.SHELL && !Objects.equals(moduleNameOf(origin), moduleNameOf(target));
                    if (otherShell) {
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

    private static final String BUILDER = "Builder";

    private static ArchCondition<JavaClass> beReadFromOutsideTheirModule() {
        return new ArchCondition<>("be read by a class outside their module") {
            @Override
            public void check(JavaClass published, ConditionEvents events) {
                if (!isReadFromOutside(published)) {
                    events.add(SimpleConditionEvent.violated(
                            published, "%s is read by no class outside %s".formatted(published.getName(), moduleNameOf(published))));
                }
            }
        };
    }

    // Read: an outside class depends on it or calls a method returning it, a sealed parent is read, it builds a read type, or a read type throws it.
    private static boolean isReadFromOutside(JavaClass type) {
        String module = moduleNameOf(type);
        boolean dependedOn = type.getDirectDependenciesToSelf().stream()
                .anyMatch(dependency -> !module.equals(moduleNameOf(dependency.getOriginClass())));
        boolean returnedOutside = type.getMethodsWithReturnTypeOfSelf().stream()
                .flatMap(method -> method.getCallsOfSelf().stream())
                .anyMatch(call -> !module.equals(moduleNameOf(call.getOriginOwner())));
        boolean variantOfARead = Stream.concat(type.getRawSuperclass().stream(), type.getRawInterfaces().stream())
                .filter(parent -> parent.getPermittedSubclasses().isPresent() && module.equals(moduleNameOf(parent)))
                .anyMatch(ArchitectureTest::isReadFromOutside);
        boolean builderOfARead = type.getSimpleName().endsWith(BUILDER)
                && type.getEnclosingClass().filter(ArchitectureTest::isReadFromOutside).isPresent();
        boolean thrownByARead = type.isAssignableTo(DomainException.class)
                && type.getConstructorCallsToSelf().stream()
                        .map(call -> call.getOriginOwner())
                        .filter(thrower -> !thrower.equals(type) && module.equals(moduleNameOf(thrower)))
                        .anyMatch(ArchitectureTest::isReadFromOutside);
        return dependedOn || returnedOutside || variantOfARead || builderOfARead || thrownByARead;
    }

    private static ArchCondition<JavaClass> reachModulesOnlyThroughPrimaryAndPublishedFromTheShell() {
        return new ArchCondition<>("reach a context or a kernel, from the shell, only through its infrastructure.primary or published package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                if (kindOf(origin) != ModuleType.SHELL) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String to = contextOrKernelOf(target);
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

    private static String contextOrKernelOf(JavaClass javaClass) {
        ModuleType kind = kindOf(javaClass);
        return kind == ModuleType.CONTEXT || kind == ModuleType.KERNEL ? moduleNameOf(javaClass) : null;
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

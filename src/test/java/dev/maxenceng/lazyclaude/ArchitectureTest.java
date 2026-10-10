package dev.maxenceng.lazyclaude;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.Optional;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

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

    // Processes, files and the network are side effects; they live behind a port, in a secondary adapter.
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

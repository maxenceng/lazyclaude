package dev.maxenceng.lazyclaude;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/*
 * The architecture, as rules that fail the build. A context is a direct subpackage of
 * dev.maxenceng.lazyclaude other than `shell` (the TUI) and `shared` (the shared kernel),
 * shaped as {published, domain, application, infrastructure.{primary, secondary}}.
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

    // Contexts are reached through their `published` package, never by importing their insides.
    @ArchTest
    static final ArchRule contexts_meet_only_through_published = classes()
            .that()
            .resideInAPackage(ROOT + ".*..")
            .should(onlyReachOtherContextsThroughPublished())
            .allowEmptyShould(true);

    // The shell composes contexts; a context that knew about the shell could not be reused or tested alone.
    @ArchTest
    static final ArchRule nothing_depends_on_the_shell = noClasses()
            .that()
            .resideOutsideOfPackage(ROOT + ".shell..")
            .and()
            .doNotHaveFullyQualifiedName(LazyClaude.class.getName())
            .should()
            .dependOnClassesThat()
            .resideInAPackage(ROOT + ".shell..")
            .allowEmptyShould(true);

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
                    boolean published = target.getPackageName().startsWith(ROOT + "." + to + ".published");
                    if (foreign && !published) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static String contextOf(JavaClass javaClass) {
        String name = javaClass.getPackageName();
        if (!name.startsWith(ROOT + ".")) {
            return null;
        }
        String context = name.substring(ROOT.length() + 1).split("\\.")[0];
        return context.equals("shell") || context.equals("shared") ? null : context;
    }
}

package dev.maxenceng.lazyclaude;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/*
 * Conventions about the tests themselves, so they are enforced rather than remembered. Separate
 * from ArchitectureTest, which imports without tests and so cannot see any of this.
 */
@AnalyzeClasses(packages = "dev.maxenceng.lazyclaude")
class TestConventionsTest {

    // A helper only one test class can reach is a fixture in the wrong place: the next test
    // retypes it with other literals and the two drift apart. It belongs in the <Type>Fixture
    // beside the type, named for the state it builds. Rule holders keep their conditions;
    // synthetic methods are every lambda a test compiles to.
    @ArchTest
    static final ArchRule unit_tests_hold_only_tests = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .haveSimpleNameEndingWith("Test")
            .and()
            .areDeclaredInClassesThat(not(holdArchitectureRules()))
            .and(not(modifier(JavaModifier.SYNTHETIC)))
            .should()
            .bePrivate();

    private static DescribedPredicate<JavaClass> holdArchitectureRules() {
        return DescribedPredicate.describe(
                "hold architecture rules", type -> type.isAnnotatedWith(AnalyzeClasses.class));
    }
}

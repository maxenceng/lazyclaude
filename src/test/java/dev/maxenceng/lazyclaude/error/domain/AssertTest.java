package dev.maxenceng.lazyclaude.error.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class AssertTest {

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    @Test
    void shouldRefuseANullValueNamingItsField() {
        assertThatThrownBy(() -> Assert.notNull("id", null))
                .isInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("id")
                .extracting(e -> ((AssertionException) e).field())
                .isEqualTo("id");
    }

    @Test
    void shouldAcceptAValueThatIsPresent() {
        assertThatCode(() -> Assert.notNull("id", "A-1")).doesNotThrowAnyException();
    }

    @Test
    void shouldRefuseAValueNotMatchingThePatternInTheWordsGiven() {
        assertThatThrownBy(() -> Assert.matchesPattern("number", "12a", DIGITS, "digits"))
                .isInstanceOf(NotMatchingExpectedPatternException.class)
                .hasMessage("number '12a' is not digits");
    }

    @Test
    void shouldRefuseANullValueBeforeMatchingIt() {
        assertThatThrownBy(() -> Assert.matchesPattern("number", null, DIGITS, "digits"))
                .isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldAcceptAValueMatchingTheWholePattern() {
        assertThatCode(() -> Assert.matchesPattern("number", "12", DIGITS, "digits")).doesNotThrowAnyException();
    }

    @Test
    void shouldReportAnAssertionAsAnInvalidInput() {
        assertThatThrownBy(() -> Assert.notNull("id", null))
                .extracting(e -> ((DomainException) e).status())
                .isEqualTo(DomainErrorStatus.INVALID);
    }

    @Test
    void shouldRefuseABlankValueNamingItsField() {
        assertThatThrownBy(() -> Assert.notBlank("title", "  "))
                .isInstanceOf(BlankValueException.class)
                .hasMessage("title is blank");
    }

    @Test
    void shouldRefuseANullValueBeforeCheckingItIsBlank() {
        assertThatThrownBy(() -> Assert.notBlank("title", null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldAcceptAValueWithText() {
        assertThatCode(() -> Assert.notBlank("title", "A board")).doesNotThrowAnyException();
    }

    @Test
    void shouldRefuseARelativePathNamingItsField() {
        assertThatThrownBy(() -> Assert.absolute("root", Path.of("app")))
                .isInstanceOf(RelativePathException.class)
                .hasMessage("root is absolute, got app");
    }

    @Test
    void shouldRefuseANullPathBeforeCheckingItIsAbsolute() {
        assertThatThrownBy(() -> Assert.absolute("root", null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldAcceptAnAbsolutePath() {
        assertThatCode(() -> Assert.absolute("root", Path.of("/work/app"))).doesNotThrowAnyException();
    }

    @Test
    void shouldRefuseAnEmptyCollectionNamingItsField() {
        assertThatThrownBy(() -> Assert.notEmpty("warnings", List.of()))
                .isInstanceOf(EmptyValueException.class)
                .hasMessage("warnings is empty");
    }

    @Test
    void shouldRefuseAMissingCollectionBeforeCheckingItIsEmpty() {
        assertThatThrownBy(() -> Assert.notEmpty("warnings", null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldAcceptACollectionWithOneElement() {
        assertThatCode(() -> Assert.notEmpty("warnings", List.of("a"))).doesNotThrowAnyException();
    }
}

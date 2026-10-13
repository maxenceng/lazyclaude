package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.bareTicket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketWithBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketTest {

    @Test
    void shouldKeepAnAbsentContextAndTypeAsAbsent() {
        Ticket ticket = bareTicket();

        assertThat(ticket.context()).isEqualTo(new NoContext());
        assertThat(ticket.type()).isEqualTo(new NoType());
    }

    @Test
    void shouldRefuseATicketWithoutAnId() {
        assertThatThrownBy(() -> ticketBuilder().id(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("id");
    }

    @Test
    void shouldRefuseATicketWithoutABranch() {
        assertThatThrownBy(() -> ticketBuilder().branch(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("branch");
    }

    @Test
    void shouldRefuseATicketWithoutAStatus() {
        assertThatThrownBy(() -> ticketBuilder().status(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseContextIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().context(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseTypeIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().type(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWithoutFrontmatter() {
        assertThatThrownBy(() -> ticketBuilder().frontmatter(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseBodyIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().body(null).build())
                .as("absence is NoBody, never null")
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldKeepAnAbsentBodyAsAbsent() {
        assertThat(bareTicket().body()).isEqualTo(new NoBody());
    }

    @Test
    void shouldTakeTheFirstHeadingOfTheBodyAsTheTitleStripped() {
        assertThat(ticketWithBody("\n#   Spaced out  \n\n# Later").title()).isEqualTo(new Title("Spaced out"));
    }

    @Test
    void shouldHaveNoTitleWhenTheBodyIsAbsent() {
        assertThat(bareTicket().title()).as("the domain does not stand the id in for a missing title").isEqualTo(new NoTitle());
    }

    @Test
    void shouldHaveNoTitleWhenTheBodyHasNoHeading() {
        assertThat(ticketWithBody("Just prose.").title()).isEqualTo(new NoTitle());
    }

    @ParameterizedTest
    @ValueSource(strings = {"```\n# not a title\n```", "~~~\n# not a title\n~~~", "```\n# not a title"})
    void shouldNotTakeAHeadingInsideACodeFenceForTheTitle(String body) {
        assertThat(ticketWithBody(body).title()).isEqualTo(new NoTitle());
    }

    @ParameterizedTest
    @ValueSource(strings = {"```\ncode\n```\n# After", "~~~\ncode\n~~~\n# After"})
    void shouldTakeAHeadingAfterTheCodeFenceClosesForTheTitle(String body) {
        assertThat(ticketWithBody(body).title()).isEqualTo(new Title("After"));
    }

    @Test
    void shouldTakeAHeadingBeforeACodeFenceForTheTitle() {
        assertThat(ticketWithBody("# Before\n```\n# Inside\n```").title()).isEqualTo(new Title("Before"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#", "#  ", "# ", "##", "## Not a title", "#Tight", " # Indented"})
    void shouldNotTakeAnythingButAHashAndASpaceFollowedByTextForTheTitle(String line) {
        assertThat(ticketWithBody(line).title()).isEqualTo(new NoTitle());
    }

    @Test
    void shouldReadAnEmptyFirstHeadingAsNoTitleEvenWhenAnotherFollows() {
        assertThat(ticketWithBody("# \n# Later").title())
                .as("the first heading is the title, even when it is empty")
                .isEqualTo(new NoTitle());
    }
}

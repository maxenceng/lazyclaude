package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.FrontmatterEntryViewFixture.datedFrontmatterViews;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.FrontmatterEntryViewFixture.frontmatterViews;
import static dev.maxenceng.lazyclaude.board.published.BodyFixture.body;
import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.secondTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.slugTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.longTitle;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView.TicketViewBuilder;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;

public final class TicketViewFixture {

    private TicketViewFixture() {}

    public static TicketViewBuilder ticketViewBuilder() {
        return TicketView.builder()
                .id(ticketId())
                .branch(ticketBranch())
                .title(title())
                .status(Status.DRAFT)
                .context(contextName())
                .type(typeName())
                .frontmatter(frontmatterViews())
                .body(body());
    }

    public static TicketView ticketView() {
        return ticketViewBuilder().build();
    }

    public static TicketView secondTicketView() {
        return ticketViewBuilder().id(secondTicketId()).branch(secondTicketBranch()).title(secondTitle()).build();
    }

    public static TicketView slugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).build();
    }

    public static TicketView datedSlugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).frontmatter(datedFrontmatterViews()).build();
    }

    public static TicketView inReviewSlugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).status(Status.IN_REVIEW).build();
    }

    public static TicketView longTitledSlugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).title(longTitle()).build();
    }

    public static TicketView doneTicketView() {
        return ticketViewBuilder().status(Status.DONE).build();
    }

    public static TicketView untitledTicketView() {
        return ticketViewBuilder().title(new NoTitle()).build();
    }

    public static TicketView bareTicketView() {
        return ticketViewBuilder().context(new NoContext()).type(new NoType()).build();
    }

    public static TicketView bodylessTicketView() {
        return ticketViewBuilder().body(new NoBody()).build();
    }

    public static TicketView datedTicketView() {
        return ticketViewBuilder().frontmatter(datedFrontmatterViews()).build();
    }

    public static TicketView longTicketView() {
        return ticketViewBuilder().body(listOfRows(60)).build();
    }

    public static TicketView longDoneTicketView() {
        return ticketViewBuilder().status(Status.DONE).body(listOfRows(60)).build();
    }

    public static TicketView longSecondTicketView() {
        return ticketViewBuilder().id(secondTicketId()).branch(secondTicketBranch()).title(secondTitle()).body(listOfRows(60)).build();
    }

    public static TicketView wrappingTicketView() {
        String sentence = "words that wrap across the width of the main view ".repeat(40);
        return ticketViewBuilder().body(new Body(sentence + "THE-END")).build();
    }

    public static TicketView thematicBreakTicketView() {
        return ticketViewBuilder().body(new Body("above\n\n---\n\nbelow")).build();
    }

    private static Body listOfRows(int count) {
        return new Body(IntStream.rangeClosed(1, count)
                .mapToObj(number -> "- Row %02d".formatted(number))
                .collect(Collectors.joining("\n")));
    }
}

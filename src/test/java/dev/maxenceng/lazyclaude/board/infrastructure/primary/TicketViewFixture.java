package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.published.BodyFixture.body;
import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.LabelNameFixture.labelNames;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.secondTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.slugTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.longTitle;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView.TicketViewBuilder;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.Verb;

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
                .otherLabels(List.of())
                .body(body())
                .verbs(List.of(Verb.REFINE));
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

    public static TicketView inReviewSlugTicketView() {
        return ticketViewBuilder()
                .branch(slugTicketBranch())
                .status(Status.IN_REVIEW)
                .verbs(Status.IN_REVIEW.verbs())
                .build();
    }

    public static TicketView longTitledSlugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).title(longTitle()).build();
    }

    public static TicketView ticketViewNumbered(int number, Status status) {
        return ticketViewBuilder()
                .id(new TicketId(String.valueOf(number)))
                .branch(new TicketBranch("PROJ-" + number))
                .title(new Title("Ticket number " + number))
                .status(status)
                .verbs(status.verbs())
                .build();
    }

    public static TicketView[] ticketViewsIn(Status status, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(number -> ticketViewNumbered(number, status))
                .toArray(TicketView[]::new);
    }

    public static TicketView doneTicketView() {
        return ticketViewBuilder().status(Status.DONE).verbs(List.of()).build();
    }

    public static TicketView ticketViewIn(Status status) {
        return ticketViewBuilder().status(status).verbs(status.verbs()).build();
    }

    public static TicketView inProgressDesignTicketView() {
        return ticketViewBuilder()
                .status(Status.IN_PROGRESS)
                .verbs(List.of(Verb.REVIEW, Verb.DESIGN))
                .build();
    }

    public static TicketView bareTicketView() {
        return ticketViewBuilder().context(new NoContext()).type(new NoType()).build();
    }

    public static TicketView bodylessTicketView() {
        return ticketViewBuilder().body(new NoBody()).build();
    }

    public static TicketView labelledTicketView() {
        return ticketViewBuilder().otherLabels(labelNames()).build();
    }

    public static TicketView labelledSlugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).otherLabels(labelNames()).build();
    }

    public static TicketView longTicketView() {
        return ticketViewBuilder().body(listOfRows(60)).build();
    }

    public static TicketView longSlugTicketView() {
        return ticketViewBuilder().branch(slugTicketBranch()).body(listOfRows(60)).build();
    }

    public static TicketView longDoneTicketView() {
        return ticketViewBuilder().status(Status.DONE).verbs(List.of()).body(listOfRows(60)).build();
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

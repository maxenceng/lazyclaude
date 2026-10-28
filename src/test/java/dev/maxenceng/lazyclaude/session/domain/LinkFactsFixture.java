package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketIds;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.TranscriptReadingFixture.readable;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.SessionId;

public final class LinkFactsFixture {

    private LinkFactsFixture() {}

    public static KnownTickets knownTickets(String... ids) {
        return new KnownTickets(linkedTicketIds(ids));
    }

    public static KnownTickets noTickets() {
        return new KnownTickets(List.of());
    }

    public static KnownTickets boardTickets() {
        return knownTickets("PROJ-1", "PROJ-2");
    }

    public static TicketRun ticketRun(String verb, String... ids) {
        return TicketRun.builder()
                .verb(new LinkVerb(verb))
                .ids(namedIds(ids))
                .build();
    }

    public static LinkFacts ran(TicketRun... runs) {
        return LinkFacts.builder().runs(List.of(runs)).build();
    }

    public static LinkFacts noRuns() {
        return ran();
    }

    public static List<NamedTicketId> namedIds(String... ids) {
        return Arrays.stream(ids).map(NamedTicketId::new).toList();
    }

    public static TranscriptFacts factsRunning(SessionId id, TicketRun... runs) {
        return TranscriptFacts.builder()
                .id(id)
                .title(aiTitle())
                .opening(firstPrompt())
                .reading(readable())
                .lastActivity(new LastActivity(now()))
                .links(ran(runs))
                .build();
    }
}

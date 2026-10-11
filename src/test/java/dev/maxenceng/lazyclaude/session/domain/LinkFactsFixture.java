package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketIds;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.StrongLink;
import dev.maxenceng.lazyclaude.session.published.TicketLinkFixture;
import dev.maxenceng.lazyclaude.session.published.WeakLink;

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
                .ids(Arrays.stream(ids).map(NamedTicketId::new).toList())
                .build();
    }

    public static LinkFacts ran(TicketRun... runs) {
        return LinkFacts.builder().runs(List.of(runs)).branchIds(List.of()).build();
    }

    public static LinkFacts onBranches(String... branchIds) {
        return LinkFacts.builder().runs(List.of()).branchIds(namedIds(branchIds)).build();
    }

    public static LinkFacts ranOnBranch(TicketRun run, String branchId) {
        return LinkFacts.builder().runs(List.of(run)).branchIds(namedIds(branchId)).build();
    }

    public static StrongLink strong(String ticket, String verb) {
        return TicketLinkFixture.strongLink(ticket, verb);
    }

    public static List<NamedTicketId> namedIds(String... ids) {
        return Arrays.stream(ids).map(NamedTicketId::new).toList();
    }

    public static WeakLink weak(String ticket) {
        return TicketLinkFixture.weakLink(ticket);
    }
}

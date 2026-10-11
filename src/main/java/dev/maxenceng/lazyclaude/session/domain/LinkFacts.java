package dev.maxenceng.lazyclaude.session.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.StrongLink;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.WeakLink;

/**
 * What a transcript says that could tie its session to a ticket, raw and in the order it was said: every {@code /ticket}
 * run that named ids, and every ticket-id prefix of a branch it recorded. Which of them count is the rule in
 * {@link #linksTo}, against whatever tickets the board has at that moment.
 */
public record LinkFacts(List<TicketRun> runs, List<NamedTicketId> branchIds) {

    public LinkFacts {
        Assert.notNull("runs", runs);
        Assert.notNull("branch ids", branchIds);
    }

    public static LinkFacts none() {
        return builder().runs(List.of()).branchIds(List.of()).build();
    }

    /**
     * The tickets the session is linked to: those its {@code /ticket} args named, else, weakly, those its branches did. Once
     * the args named an id the branches are ignored, even when every id named misses the board, so such a session is
     * unlinked rather than linked by where it happened to run.
     */
    // deferred: the pr-link rule, a session linked through the head branch of a ticket's pull request — close it when review publishes head branches.
    public List<TicketLink> linksTo(KnownTickets tickets) {
        return runs.isEmpty() ? weakLinks(tickets) : strongLinks(tickets);
    }

    private List<TicketLink> strongLinks(KnownTickets tickets) {
        Map<LinkedTicketId, LinkVerb> lastVerbs = new LinkedHashMap<>();
        for (TicketRun run : runs) {
            for (NamedTicketId named : run.ids()) {
                tickets.find(named).ifPresent(ticket -> lastVerbs.put(ticket, run.verb()));
            }
        }
        List<TicketLink> links = new ArrayList<>();
        lastVerbs.forEach((ticket, verb) -> links.add(StrongLink.builder().ticket(ticket).verb(verb).build()));
        return links;
    }

    private List<TicketLink> weakLinks(KnownTickets tickets) {
        return branchIds.stream()
                .map(tickets::find)
                .flatMap(Optional::stream)
                .distinct()
                .<TicketLink>map(WeakLink::new)
                .toList();
    }

    public static LinkFactsBuilder builder() {
        return new LinkFactsBuilder();
    }

    public static final class LinkFactsBuilder {
        private List<TicketRun> runs;
        private List<NamedTicketId> branchIds;

        private LinkFactsBuilder() {}

        public LinkFactsBuilder runs(List<TicketRun> runs) {
            this.runs = runs;
            return this;
        }

        public LinkFactsBuilder branchIds(List<NamedTicketId> branchIds) {
            this.branchIds = branchIds;
            return this;
        }

        public LinkFacts build() {
            return new LinkFacts(runs, branchIds);
        }
    }
}

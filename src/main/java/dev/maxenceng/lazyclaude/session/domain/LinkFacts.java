package dev.maxenceng.lazyclaude.session.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/**
 * What a session's {@code /ticket} runs say that could tie it to a ticket, raw and in the order they were said: the launch
 * that began it, then every run its transcript records. Which tickets count is the rule in {@link #linksTo}, against whatever
 * tickets the board has at that moment.
 */
public record LinkFacts(List<TicketRun> runs) {

    public LinkFacts {
        Assert.notNull("runs", runs);
    }

    /** These facts after {@code launch}, the run the session began as, which the runs its transcript records follow. */
    public LinkFacts launchedAs(Optional<TicketRun> launch) {
        return new LinkFacts(Stream.concat(launch.stream(), runs.stream()).toList());
    }

    /**
     * The tickets the session is linked to: those its runs named that the board has, each with the verb of the last run to
     * name it.
     */
    // deferred: the pr-link rule, a session linked through the head branch of a ticket's pull request — close it when review publishes head branches.
    public List<TicketLink> linksTo(KnownTickets tickets) {
        Map<LinkedTicketId, LinkVerb> lastVerbs = runs.stream()
                .flatMap(run -> run.ids().stream()
                        .map(tickets::find)
                        .flatMap(Optional::stream)
                        .map(ticket -> Map.entry(ticket, run.verb())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, last) -> last, LinkedHashMap::new));
        return lastVerbs.entrySet().stream()
                .map(link -> TicketLink.builder().ticket(link.getKey()).verb(link.getValue()).build())
                .toList();
    }

    public static LinkFactsBuilder builder() {
        return new LinkFactsBuilder();
    }

    public static final class LinkFactsBuilder {
        private List<TicketRun> runs = List.of();

        private LinkFactsBuilder() {}

        public LinkFactsBuilder runs(List<TicketRun> runs) {
            this.runs = runs;
            return this;
        }

        public LinkFacts build() {
            return new LinkFacts(runs);
        }
    }
}

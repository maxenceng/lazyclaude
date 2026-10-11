package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.session.domain.NamedTicketId;
import dev.maxenceng.lazyclaude.session.domain.TicketRun;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;

/**
 * A {@code /ticket <verb> <ID…>} the session ran, as the cache holds it: the first word of the args and every later word
 * that has the shape of a ticket id, raw. Args that name no id say nothing of a ticket and are not a run.
 */
record CachedTicketRun(String verb, List<String> ids) {

    private static final Pattern WORDS = Pattern.compile("\\s+");
    private static final int VERB = 0;
    private static final int FIRST_ID = VERB + 1;
    private static final int VERB_AND_ONE_ID = FIRST_ID + 1;

    boolean isWhole() {
        return verb != null && !verb.isBlank() && ids != null && !ids.isEmpty() && ids.stream().allMatch(id -> id != null && !id.isBlank());
    }

    static Optional<CachedTicketRun> parse(String args) {
        String[] words = WORDS.split(args.strip());
        if (words.length < VERB_AND_ONE_ID || NamedTicketId.fromWord(words[VERB]).isPresent()) {
            return Optional.empty();
        }
        List<String> ids = Arrays.stream(words)
                .skip(FIRST_ID)
                .map(NamedTicketId::fromWord)
                .flatMap(Optional::stream)
                .map(NamedTicketId::value)
                .toList();
        return ids.isEmpty() ? Optional.empty() : Optional.of(new CachedTicketRun(words[VERB], ids));
    }

    TicketRun toDomain() {
        return TicketRun.builder()
                .verb(new LinkVerb(verb))
                .ids(ids.stream().map(NamedTicketId::new).toList())
                .build();
    }
}

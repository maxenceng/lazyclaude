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
    private static final Pattern ID = Pattern.compile("[A-Za-z][A-Za-z0-9]*-\\d{1,18}");

    boolean isWhole() {
        return verb != null && !verb.isBlank() && ids != null && !ids.isEmpty() && ids.stream().allMatch(id -> id != null && !id.isBlank());
    }

    static Optional<CachedTicketRun> parse(String args) {
        String[] words = WORDS.split(args.strip());
        if (words.length < 2 || ID.matcher(words[0]).matches()) {
            return Optional.empty();
        }
        List<String> ids = Arrays.stream(words).skip(1).filter(word -> ID.matcher(word).matches()).toList();
        return ids.isEmpty() ? Optional.empty() : Optional.of(new CachedTicketRun(words[0], ids));
    }

    TicketRun toDomain() {
        return TicketRun.builder()
                .verb(new LinkVerb(verb))
                .ids(ids.stream().map(NamedTicketId::new).toList())
                .build();
    }
}

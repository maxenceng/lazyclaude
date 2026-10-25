package dev.maxenceng.lazyclaude.shell;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The folded rows of a panel after one is folded or unfolded, as a new set rather than a changed one. */
final class FoldedKeys {

    private FoldedKeys() {}

    static Set<RowKey> unfolded(Set<RowKey> folded, RowKey key) {
        return folded.stream().filter(Predicate.not(key::equals)).collect(Collectors.toUnmodifiableSet());
    }

    static Set<RowKey> toggled(Set<RowKey> folded, RowKey key) {
        Stream<RowKey> others = folded.stream().filter(Predicate.not(key::equals));
        return (folded.contains(key) ? others : Stream.concat(others, Stream.of(key)))
                .collect(Collectors.toUnmodifiableSet());
    }
}

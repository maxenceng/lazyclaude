package dev.maxenceng.lazyclaude.shell;

import java.util.stream.Stream;

/** A value that is part of a list only under a condition, so a list of such values is built by a stream, not by adding. */
final class Included {

    private Included() {}

    static <T> Stream<T> when(boolean included, T value) {
        return included ? Stream.of(value) : Stream.empty();
    }
}

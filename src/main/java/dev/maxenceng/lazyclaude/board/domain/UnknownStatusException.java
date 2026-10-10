package dev.maxenceng.lazyclaude.board.domain;

import java.util.Arrays;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A ticket's status is not one of the five. */
public final class UnknownStatusException extends DomainException {

    UnknownStatusException(String label) {
        super(DomainErrorStatus.INVALID, "status '%s' is not one of %s".formatted(label, labels()));
    }

    private static String labels() {
        return String.join(", ", Arrays.stream(Status.values()).map(Status::label).toList());
    }
}

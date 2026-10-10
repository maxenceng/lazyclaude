package dev.maxenceng.lazyclaude.error.domain;

/** What kind of failure a {@link DomainException} represents, in business terms. */
public enum DomainErrorStatus {
    /** The thing referred to does not exist. */
    NOT_FOUND,

    /** The input itself is not valid, whatever the current state is. */
    INVALID,

    /** Something this depends on did not answer; the same request may work later. */
    UNAVAILABLE
}

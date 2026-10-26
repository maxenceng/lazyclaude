package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/**
 * The configuration file is not valid; the message is {@code <file>:<line>: <reason>}, or {@code <file>: <reason>} when the whole
 * file cannot be read, and lazyclaude does not start.
 */
public final class ConfigurationRefusedException extends DomainException {

    public ConfigurationRefusedException(String reason) {
        super(DomainErrorStatus.INVALID, reason);
    }
}

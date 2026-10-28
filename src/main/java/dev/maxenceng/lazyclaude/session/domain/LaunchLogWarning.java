package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Warning;

/** The one wording of a launch log that cannot be used, whichever manager meets it first. */
final class LaunchLogWarning {

    private static final String WORDING = "launch log: %s.";

    private LaunchLogWarning() {}

    static Warning of(Reason cause) {
        return new Warning(WORDING.formatted(cause.value()));
    }
}

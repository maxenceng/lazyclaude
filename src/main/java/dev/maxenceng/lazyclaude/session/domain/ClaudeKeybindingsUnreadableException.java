package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Claude Code's keybindings could not be read, or are not of the shape it writes, so the clash check is skipped and lazyclaude starts anyway. */
public final class ClaudeKeybindingsUnreadableException extends DomainException {

    public ClaudeKeybindingsUnreadableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public ClaudeKeybindingsUnreadableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

/** The registry lists the hosted session's pid: this is the live session that record describes. */
public record LivePhase(LiveSession session) implements HostedPhase {

    public LivePhase {
        Assert.notNull("session", session);
    }

    @Override
    public boolean isBusyOrWaiting() {
        return switch (session.state()) {
            case Busy _ -> true;
            case WaitingForPermission _ -> true;
            case WaitingForInput _ -> true;
            case YourTurn _ -> false;
            case Unknown _ -> false;
        };
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

/** Spawned, with no registry record yet: just started, or still on Claude Code's trust dialog. It asks nothing of you. */
public record StartingPhase() implements HostedPhase {

    @Override
    public boolean isBusyOrWaiting() {
        return false;
    }

    @Override
    public Optional<LiveSession> live() {
        return Optional.empty();
    }

    @Override
    public HostedPhase showing(LiveSession session) {
        return this;
    }
}

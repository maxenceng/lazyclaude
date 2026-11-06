package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.ConfiguredVerbsPort;
import dev.maxenceng.lazyclaude.board.published.StatusVerbs;

/** The verbs read from {@code config.yml} once at start-up, which the composition root hands in. */
public final class ConfiguredVerbsRepository implements ConfiguredVerbsPort {

    private final List<StatusVerbs> configured;

    public ConfiguredVerbsRepository(List<StatusVerbs> configured) {
        this.configured = configured;
    }

    @Override
    public List<StatusVerbs> configured() {
        return configured;
    }
}

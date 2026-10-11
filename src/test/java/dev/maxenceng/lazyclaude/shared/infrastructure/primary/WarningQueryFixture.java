package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;

public final class WarningQueryFixture {

    private WarningQueryFixture() {}

    public static WarningQuery warningQuery(WarningWatchPort watch) {
        return new WarningQuery(new WarningApplicationService(watch));
    }
}

package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;

public final class WarningCommandFixture {

    private WarningCommandFixture() {}

    public static WarningCommand warningCommand(WarningRepository raised) {
        return new WarningCommand(new WarningApplicationService(raised, raised));
    }
}

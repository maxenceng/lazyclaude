package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;

public final class WarningQueryFixture {

    private WarningQueryFixture() {}

    public static WarningQuery warningQuery(WarningRepository raised) {
        return new WarningQuery(new WarningApplicationService(raised, raised));
    }
}

package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.application.DocsApplicationService;
import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class DocsQueryFixture {

    private DocsQueryFixture() {}

    public static DocsQuery docsQuery(DocsPort docs, DocsWatchPort watch, WarningPort warnings) {
        return new DocsQuery(new DocsApplicationService(docs, watch, warnings));
    }
}

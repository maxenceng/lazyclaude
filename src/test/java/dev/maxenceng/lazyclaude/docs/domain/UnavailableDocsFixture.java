package dev.maxenceng.lazyclaude.docs.domain;

import java.io.IOException;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.docs.published.Reason;

public final class UnavailableDocsFixture {

    private UnavailableDocsFixture() {}

    public static DocsUnavailableException docsUnavailable() {
        return new DocsUnavailableException(Path.of("/work/app/docs"), new IOException("denied"));
    }

    public static UnavailableDocs unavailableDocs() {
        return new UnavailableDocs(new Reason("cannot list /work/app/docs: denied"));
    }
}

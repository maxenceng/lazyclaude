package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.projectPath;

import java.util.function.Consumer;

import org.mockito.stubbing.Answer;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;

public final class DocsSidecarFixture {

    private DocsSidecarFixture() {}

    public static DocsPayload docsPayload() {
        return new DocsPayload(projectPath());
    }

    public static String docsParams() {
        return "{\"root\":\"%s\"}".formatted(projectPath());
    }

    public static String otherDocsParams() {
        return "{\"root\":\"%s\"}".formatted(SidecarFixture.otherProjectPath());
    }

    public static String emptyDocs() {
        return "{\"kind\":\"docsTree\",\"documents\":[],\"folders\":[]}";
    }

    public static String unavailableDocs() {
        return "{\"kind\":\"unavailableDocs\",\"reason\":\"cannot list /work/app/docs: denied\"}";
    }

    public static String docsChanged(String docs) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"docs/changed\",\"params\":%s}".formatted(docs);
    }

    public static Answer<Object> docsChangingAtOnce(DocsView view) {
        return call -> {
            call.<Consumer<DocsView>>getArgument(1).accept(view);
            return null;
        };
    }
}

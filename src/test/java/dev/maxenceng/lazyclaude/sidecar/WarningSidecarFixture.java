package dev.maxenceng.lazyclaude.sidecar;

import java.util.function.Consumer;

import org.mockito.stubbing.Answer;

import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningsView;

public final class WarningSidecarFixture {

    private WarningSidecarFixture() {}

    public static String oneWarning() {
        return "{\"kind\":\"warning\",\"text\":\"docs: cannot watch docs/ (inotify limit); checking every 5 s\",\"count\":1}";
    }

    public static String twoWarnings() {
        return "{\"kind\":\"warning\",\"text\":\"sessions: cannot watch the registry; checking every 2 s\",\"count\":2}";
    }

    public static String noWarning() {
        return "{\"kind\":\"noWarning\"}";
    }

    public static String warningsChanged(String warnings) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"warning/changed\",\"params\":%s}".formatted(warnings);
    }

    public static Answer<Object> warningsAtOnce(WarningsView view) {
        return call -> {
            call.<Consumer<WarningsView>>getArgument(0).accept(view);
            return null;
        };
    }
}

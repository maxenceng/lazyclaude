package dev.maxenceng.lazyclaude.shared.domain;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.mockito.ArgumentCaptor;

public final class WarningFixture {

    private WarningFixture() {}

    public static Warning warning() {
        return new Warning("docs: cannot watch docs/ (inotify limit); checking every 5 s");
    }

    public static List<String> raisedTexts(WarningPort port) {
        ArgumentCaptor<Warning> raised = ArgumentCaptor.forClass(Warning.class);
        verify(port, atLeast(0)).raise(raised.capture());
        return raised.getAllValues().stream().map(Warning::text).toList();
    }

    public static Warning otherWarning() {
        return new Warning("sessions: cannot watch the registry; checking every 2 s");
    }
}

package dev.maxenceng.lazyclaude.shell;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import dev.tamboui.terminal.Backend;

final class ScriptedBackendFixture {

    private static final int TIMED_OUT = -2;

    private ScriptedBackendFixture() {}

    static Backend backendTyping(String input) throws IOException {
        Deque<Integer> waiting = new ArrayDeque<>(input.chars().boxed().toList());
        Backend backend = mock(Backend.class);
        when(backend.read(anyInt())).thenAnswer(call -> waiting.isEmpty() ? TIMED_OUT : waiting.pollFirst());
        when(backend.peek(anyInt())).thenAnswer(call -> waiting.isEmpty() ? TIMED_OUT : waiting.peekFirst());
        return backend;
    }
}

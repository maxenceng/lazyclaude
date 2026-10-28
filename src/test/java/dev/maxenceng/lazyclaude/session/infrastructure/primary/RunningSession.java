package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.SessionId;

public record RunningSession(SessionId id, Consumer<PtyBytes> output, Consumer<ExitCode> exit) {}

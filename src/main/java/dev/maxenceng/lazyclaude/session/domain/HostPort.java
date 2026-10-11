package dev.maxenceng.lazyclaude.session.domain;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Where {@code claude} runs on a pty of lazyclaude's own, and how it is fed and read back. */
public interface HostPort {

    /**
     * Runs {@code launch} on a pty and tells {@code onExit} the code when the process ends, on a thread of the host's own.
     *
     * @throws HostUnavailableException when nothing could be started, with the reason
     */
    ProcessId spawn(HostLaunch launch, Consumer<ExitCode> onExit);

    /** Whether {@code directory} exists, so a process can be started in it. */
    boolean exists(WorkingDirectory directory);

    /** Hands the session a key, a paste or a mouse event, as the bytes a terminal would send; nothing for a session that is gone. */
    void send(HostedSessionId id, TerminalInput input);

    /** Gives the session's pty and emulator a new size. */
    void resize(HostedSessionId id, PaneSize size);

    /** What the session shows right now; blank for a session that is gone. */
    TerminalScreen screenOf(HostedSessionId id);

    /** Ends the session's process. */
    void end(HostedSessionId id);

    /** Tells {@code listener} which session drew something, on a thread of the host's own. */
    void onScreenChange(Consumer<HostedSessionId> listener);
}

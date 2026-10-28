package dev.maxenceng.lazyclaude.session.domain;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Where {@code claude} runs on a pty of lazyclaude's own, and the bytes that cross it; the host never emulates. */
public interface HostPort {

    /**
     * Runs {@code launch} on a pty. {@code onOutput} gets each read of the child's output, and {@code onExit} the code when the
     * process ends, both on one thread of the host's own, so the exit always comes after the last output.
     *
     * @throws HostUnavailableException when nothing could be started, with the reason
     */
    ProcessId spawn(HostLaunch launch, Consumer<PtyBytes> onOutput, Consumer<ExitCode> onExit);

    /** Whether {@code directory} exists, so a process can be started in it. */
    boolean exists(WorkingDirectory directory);

    /** Writes {@code bytes} to the session's pty unchanged; nothing for a session that is gone. */
    void write(SessionId id, PtyBytes bytes);

    /** Gives the session's pty a new size. */
    void resize(SessionId id, PaneSize size);

    /** Ends the session's process. */
    void end(SessionId id);
}

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
     * process ends, both on one thread of the host's own, so the exit always comes after the last output. {@code fallback} hears,
     * once per run, that what the host loads on its first spawn could not be kept across runs; the session starts all the same.
     *
     * @throws HostUnavailableException when nothing could be started, with the reason
     */
    ProcessId spawn(HostLaunch launch, Consumer<PtyBytes> onOutput, Consumer<ExitCode> onExit, HostFallback fallback);

    /** Whether {@code directory} exists, so a process can be started in it. */
    boolean exists(WorkingDirectory directory);

    /** Writes {@code bytes} to the session's pty unchanged; nothing for a session that is gone. */
    void write(SessionId id, PtyBytes bytes);

    /** Gives the session's pty a new size. */
    void resize(SessionId id, PaneSize size);

    /** Ends the session's process, returning only once it has, so a quit leaves no {@code claude} behind. */
    void end(SessionId id);

    /**
     * Asks the session's process to end and returns at once, so whoever asked is not held up for the grace; one still running
     * once the grace runs out is killed on a thread of the host's own.
     */
    void close(SessionId id);
}

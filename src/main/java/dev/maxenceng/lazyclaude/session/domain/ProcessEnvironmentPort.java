package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** Where the environment a process runs with is read from. */
public interface ProcessEnvironmentPort {

    /**
     * The environment {@code pid} was started with; empty once the process is gone.
     *
     * @throws EnvironmentUnreadableException when the process is there but its environment cannot be read, with the reason
     */
    ProcessEnvironment of(ProcessId pid);

    /** The environment lazyclaude itself runs with. */
    ProcessEnvironment ownEnvironment();
}

package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** The hosted sessions as last read from the registry, so a change to the transcripts lists them without reading it again. */
public final class LastLiveRead {

    private final AtomicReference<Optional<RunningSessions>> last = new AtomicReference<>(Optional.empty());

    public void remember(RunningSessions running) {
        last.set(Optional.of(running));
    }

    /** The last read, when it was of {@code project}. */
    public Optional<RunningSessions> of(Project project) {
        return last.get().filter(running -> running.project().equals(project));
    }
}

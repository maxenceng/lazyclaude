package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The sessions lazyclaude started in a project, one entry per session in the order they were logged. */
public record LaunchLog(List<LaunchEntry> entries) {

    public LaunchLog {
        Assert.notNull("entries", entries);
    }

    /** This log with {@code entry} appended, or, for a session already logged, that session's entry with its end cleared. */
    public LaunchLog launched(LaunchEntry entry) {
        if (entryOf(entry.id()).isPresent()) {
            return new LaunchLog(entries.stream()
                    .map(logged -> logged.id().equals(entry.id()) ? logged.resumed() : logged)
                    .toList());
        }
        return new LaunchLog(Stream.concat(entries.stream(), Stream.of(entry)).toList());
    }

    public LaunchLog ended(SessionId id, EndedAt ended) {
        return new LaunchLog(entries.stream()
                .map(logged -> logged.id().equals(id) ? logged.endedAt(ended) : logged)
                .toList());
    }

    public LaunchLog without(Set<SessionId> ids) {
        return new LaunchLog(entries.stream().filter(logged -> !ids.contains(logged.id())).toList());
    }

    public Set<SessionId> ids() {
        return entries.stream().map(LaunchEntry::id).collect(Collectors.toUnmodifiableSet());
    }

    public Optional<LaunchEntry> entryOf(SessionId id) {
        return entries.stream().filter(logged -> logged.id().equals(id)).findFirst();
    }

    public static LaunchLogBuilder builder() {
        return new LaunchLogBuilder();
    }

    public static final class LaunchLogBuilder {
        private List<LaunchEntry> entries = List.of();

        private LaunchLogBuilder() {}

        public LaunchLogBuilder entries(List<LaunchEntry> entries) {
            this.entries = entries;
            return this;
        }

        public LaunchLog build() {
            return new LaunchLog(entries);
        }
    }
}

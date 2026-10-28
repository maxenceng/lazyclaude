package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** One launch lazyclaude made: the {@code claude} it ran and where, when it started and how it ended. */
public record LaunchEntry(LaunchCommand command, WorkingDirectory directory, StartedAt startedAt, LaunchEnd end) {

    public LaunchEntry {
        Assert.notNull("command", command);
        Assert.notNull("directory", directory);
        Assert.notNull("started at", startedAt);
        Assert.notNull("end", end);
    }

    /** The session the launch began or picked up again. */
    public SessionId id() {
        return command.session();
    }

    public LaunchEntry endedAt(EndedAt ended) {
        return new LaunchEntry(command, directory, startedAt, ended);
    }

    /** This entry with its end cleared, as it is when its session is hosted again. */
    public LaunchEntry resumed() {
        return new LaunchEntry(command, directory, startedAt, new NotEnded());
    }

    /** Whether no end was recorded. */
    public boolean hasNoEnd() {
        return end instanceof NotEnded;
    }

    public static LaunchEntryBuilder builder() {
        return new LaunchEntryBuilder();
    }

    public static final class LaunchEntryBuilder {
        private LaunchCommand command;
        private WorkingDirectory directory;
        private StartedAt startedAt;
        private LaunchEnd end = new NotEnded();

        private LaunchEntryBuilder() {}

        public LaunchEntryBuilder command(LaunchCommand command) {
            this.command = command;
            return this;
        }

        public LaunchEntryBuilder directory(WorkingDirectory directory) {
            this.directory = directory;
            return this;
        }

        public LaunchEntryBuilder startedAt(StartedAt startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public LaunchEntryBuilder end(LaunchEnd end) {
            this.end = end;
            return this;
        }

        public LaunchEntry build() {
            return new LaunchEntry(command, directory, startedAt, end);
        }
    }
}

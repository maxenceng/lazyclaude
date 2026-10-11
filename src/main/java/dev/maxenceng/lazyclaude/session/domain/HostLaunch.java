package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** What a host is asked to run: which {@code claude}, where, and on a pty of what size, under the id its tab will carry. */
public record HostLaunch(HostedSessionId id, LaunchCommand command, WorkingDirectory directory, PaneSize size) {

    public HostLaunch {
        Assert.notNull("hosted session id", id);
        Assert.notNull("command", command);
        Assert.notNull("directory", directory);
        Assert.notNull("size", size);
    }

    public static HostLaunchBuilder builder() {
        return new HostLaunchBuilder();
    }

    public static final class HostLaunchBuilder {
        private HostedSessionId id;
        private LaunchCommand command;
        private WorkingDirectory directory;
        private PaneSize size;

        private HostLaunchBuilder() {}

        public HostLaunchBuilder id(HostedSessionId id) {
            this.id = id;
            return this;
        }

        public HostLaunchBuilder command(LaunchCommand command) {
            this.command = command;
            return this;
        }

        public HostLaunchBuilder directory(WorkingDirectory directory) {
            this.directory = directory;
            return this;
        }

        public HostLaunchBuilder size(PaneSize size) {
            this.size = size;
            return this;
        }

        public HostLaunch build() {
            return new HostLaunch(id, command, directory, size);
        }
    }
}

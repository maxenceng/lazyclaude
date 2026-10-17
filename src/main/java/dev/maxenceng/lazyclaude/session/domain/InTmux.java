package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ResidenceName;

/** The session runs in a tmux pane, of the server its environment names, if it does. */
public record InTmux(TmuxPane pane, TmuxServer server) implements Residence {

    private static final ResidenceName NAME = new ResidenceName("tmux");

    public InTmux {
        Assert.notNull("tmux pane", pane);
        Assert.notNull("tmux server", server);
    }

    @Override
    public Optional<ResidenceName> name() {
        return Optional.of(NAME);
    }

    /** Whether {@code other}'s pane belongs to this server, so that a client here can switch to it. */
    public boolean servesPaneOf(InTmux other) {
        return server.isSameAs(other.server);
    }

    public static InTmuxBuilder builder() {
        return new InTmuxBuilder();
    }

    public static final class InTmuxBuilder {
        private TmuxPane pane;
        private TmuxServer server;

        private InTmuxBuilder() {}

        public InTmuxBuilder pane(TmuxPane pane) {
            this.pane = pane;
            return this;
        }

        public InTmuxBuilder server(TmuxServer server) {
            this.server = server;
            return this;
        }

        public InTmux build() {
            return new InTmux(pane, server);
        }
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A tmux server told by the socket its clients reach it through, the first field of {@code TMUX}. */
public record TmuxSocket(String path) implements TmuxServer {

    public TmuxSocket {
        Assert.notBlank("tmux socket", path);
    }

    @Override
    public boolean isSameAs(TmuxServer other) {
        return equals(other);
    }
}

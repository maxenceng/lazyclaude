package dev.maxenceng.lazyclaude.session.domain;

/** The process carries a pane but not the server it belongs to. */
public record UnknownTmuxServer() implements TmuxServer {

    @Override
    public boolean isSameAs(TmuxServer other) {
        return false;
    }
}

package dev.maxenceng.lazyclaude.session.domain;


/** kitty's configuration could not be read; the message says why, for the person reading the status line. */
public final class KittyConfigUnreadableException extends KittyConfigException {

    private static final long serialVersionUID = 1L;

    public KittyConfigUnreadableException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

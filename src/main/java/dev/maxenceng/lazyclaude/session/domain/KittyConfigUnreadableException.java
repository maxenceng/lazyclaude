package dev.maxenceng.lazyclaude.session.domain;


/** kitty's configuration could not be read; the message says why, for the person reading the status line. */
public final class KittyConfigUnreadableException extends KittyConfigException {

    public KittyConfigUnreadableException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

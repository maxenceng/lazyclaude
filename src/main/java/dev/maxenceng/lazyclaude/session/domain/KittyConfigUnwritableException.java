package dev.maxenceng.lazyclaude.session.domain;


/** kitty's configuration could not be added to; the message says why, for the person reading the status line. */
public final class KittyConfigUnwritableException extends KittyConfigException {

    public KittyConfigUnwritableException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

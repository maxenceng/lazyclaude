package dev.maxenceng.lazyclaude.sidecar;

/** A View whose component cannot be read to be written; the protocol grows with the Views, so this is a mistake in one of them (ADR 0050). */
final class ViewUnreadableException extends RuntimeException {

    ViewUnreadableException(String message, Throwable cause) {
        super(message, cause);
    }
}

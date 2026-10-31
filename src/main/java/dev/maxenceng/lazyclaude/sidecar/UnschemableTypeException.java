package dev.maxenceng.lazyclaude.sidecar;

/** A type of a View or Payload the schema cannot state; it has to change shape before it reaches the protocol (ADR 0073). */
final class UnschemableTypeException extends RuntimeException {

    private static final String CANNOT_STATE = "the protocol cannot state %s: %s";

    UnschemableTypeException(String type, String reason) {
        super(CANNOT_STATE.formatted(type, reason));
    }
}

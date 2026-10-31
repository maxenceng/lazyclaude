package dev.maxenceng.lazyclaude.sidecar;

/** A type of a View or Payload the schema cannot state; it has to change shape before it reaches the protocol (ADR 0050). */
final class UnschemableTypeException extends RuntimeException {

    UnschemableTypeException(String message) {
        super(message);
    }
}

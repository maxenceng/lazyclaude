package dev.maxenceng.lazyclaude.sidecar;

/** Two different types of the protocol share a simple name, which keys the schema's definitions (ADR 0073). */
final class DuplicateSchemaNameException extends RuntimeException {

    DuplicateSchemaNameException(String message) {
        super(message);
    }
}

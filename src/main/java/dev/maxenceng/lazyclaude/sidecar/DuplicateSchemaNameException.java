package dev.maxenceng.lazyclaude.sidecar;

/** Two different types of one module share a simple name, which with the module keys the schema's definitions (ADR 0073). */
final class DuplicateSchemaNameException extends RuntimeException {

    DuplicateSchemaNameException(String message) {
        super(message);
    }
}

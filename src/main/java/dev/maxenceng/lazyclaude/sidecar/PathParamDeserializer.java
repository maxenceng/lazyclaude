package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Reads a path param as {@link Path#of}, since the schema states a path as any string: Jackson's own reader takes a colon for a URI
 * scheme and refuses {@code /tmp/a:b} (ADR 0073).
 */
final class PathParamDeserializer extends StdScalarDeserializer<Path> {

    private static final String NOT_A_PATH = "not a path: %s";

    PathParamDeserializer() {
        super(Path.class);
    }

    @Override
    public Path deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (Path) context.handleUnexpectedToken(Path.class, parser);
        }
        String text = parser.getString();
        try {
            return Path.of(text);
        } catch (InvalidPathException invalid) {
            return (Path) context.handleWeirdStringValue(Path.class, text, NOT_A_PATH.formatted(invalid.getReason()));
        }
    }
}

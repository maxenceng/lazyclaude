package dev.maxenceng.lazyclaude.sidecar;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The schema's {@code $defs}: each named type defined once, under its module and simple name, and referred to everywhere else, so two
 * contexts may own a type of one name and one module may not (ADR 0073).
 */
final class SchemaDefinitions {

    private static final String REF = "$ref";
    private static final String REF_PREFIX = "#/$defs/";
    private static final String SAME_NAME = "%s and %s share the name %s, which keys the schema's definitions";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final String ROOT =
            SchemaDefinitions.class.getPackageName().substring(0, SchemaDefinitions.class.getPackageName().lastIndexOf(PACKAGE_SEPARATOR));
    private static final String NO_MODULE = "it is in no module of " + ROOT + ", which qualifies the schema's definitions";
    private static final String MODULE_PREFIX = ROOT + PACKAGE_SEPARATOR;
    private static final String QUALIFIED = "%s.%s";

    private final Map<String, Class<?>> owners = new TreeMap<>();
    private final Map<String, JsonNode> definitions = new TreeMap<>();

    /** A reference to {@code type}'s definition, written from {@code definition} the first time the type is met. */
    JsonNode define(Class<?> type, Supplier<JsonNode> definition) {
        String name = QUALIFIED.formatted(moduleOf(type), type.getSimpleName());
        Class<?> owner = owners.putIfAbsent(name, type);
        if (owner != null && !owner.equals(type)) {
            throw new DuplicateSchemaNameException(SAME_NAME.formatted(owner.getName(), type.getName(), name));
        }
        if (owner == null) {
            definitions.put(name, definition.get());
        }
        ObjectNode reference = SidecarJson.object();
        reference.put(REF, REF_PREFIX + name);
        return reference;
    }

    /** Every definition met so far, by name. */
    ObjectNode all() {
        ObjectNode defs = SidecarJson.object();
        definitions.forEach(defs::set);
        return defs;
    }

    /** The direct subpackage of the root a type is in. */
    private static String moduleOf(Class<?> type) {
        if (!type.getPackageName().startsWith(MODULE_PREFIX)) {
            throw new UnschemableTypeException(type.getTypeName(), NO_MODULE);
        }
        String below = type.getPackageName().substring(MODULE_PREFIX.length());
        int end = below.indexOf(PACKAGE_SEPARATOR);
        return end < 0 ? below : below.substring(0, end);
    }
}

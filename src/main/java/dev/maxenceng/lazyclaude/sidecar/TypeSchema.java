package dev.maxenceng.lazyclaude.sidecar;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The schema of a Java type, by the rules {@link SidecarJson} writes it with (ADR 0073). Results and notification params are written;
 * params are read by Jackson into the Payload's components, so a Payload is an object of all of them. The writer looks at a value's
 * runtime class and this at a declared type, so a declared type whose values could be written another way than it states (an
 * interface, an abstract class, an array other than bytes) is refused rather than guessed. Named types go to {@link SchemaDefinitions}.
 */
final class TypeSchema {

    private static final String STRING = "string";
    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    private static final String BOOLEAN = "boolean";
    private static final String NULL = "null";
    private static final String ARRAY = "array";
    private static final String ITEMS = "items";
    private static final String ANY_OF = "anyOf";
    private static final String ONE_OF = "oneOf";
    private static final String ENUM = "enum";
    private static final String CONTENT_ENCODING = "contentEncoding";
    private static final String BASE64 = "base64";
    private static final String DISCRIMINATOR = "discriminator";
    private static final String PROPERTY_NAME = "propertyName";
    private static final Set<Class<?>> INTEGERS =
            Set.of(int.class, long.class, short.class, byte.class, Integer.class, Long.class, Short.class, Byte.class);
    private static final String NOT_JSON = "its shape has to change before it is written as JSON";
    private static final String NOT_WRITTEN_ONE_WAY = "its values are written by their own class, which it does not name";
    private static final String NO_KIND = "a variant that is not a record or a sealed type carries no kind";
    private static final String HOLDS_ITSELF = "a record of one component is written as that component, so it cannot hold itself";

    private final SchemaDefinitions definitions;
    private final Set<Class<?>> unwrapping = new HashSet<>();

    TypeSchema(SchemaDefinitions definitions) {
        this.definitions = definitions;
    }

    /** The schema of the params a Payload is read from. */
    JsonNode read(Class<?> payload) {
        return definitions.define(payload, () -> {
            ObjectNode properties = SidecarJson.object();
            Arrays.stream(payload.getRecordComponents()).forEach(component -> properties.set(component.getName(), written(component.getGenericType())));
            return SchemaNodes.closedObject(properties);
        });
    }

    /** The schema of a value written as {@code type}. */
    JsonNode written(Type type) {
        return switch (type) {
            case ParameterizedType generic -> writtenGeneric(generic);
            case Class<?> plain -> writtenClass(plain);
            default -> throw new UnschemableTypeException(type.getTypeName(), NOT_JSON);
        };
    }

    private JsonNode writtenGeneric(ParameterizedType generic) {
        Class<?> raw = (Class<?>) generic.getRawType();
        Type argument = generic.getActualTypeArguments()[0];
        if (Optional.class.equals(raw)) {
            ObjectNode node = SidecarJson.object();
            node.set(ANY_OF, SidecarJson.array(List.of(written(argument), SchemaNodes.typed(NULL))));
            return node;
        }
        if (Collection.class.isAssignableFrom(raw)) {
            ObjectNode node = SchemaNodes.typed(ARRAY);
            node.set(ITEMS, written(argument));
            return node;
        }
        throw new UnschemableTypeException(generic.getTypeName(), NOT_JSON);
    }

    private JsonNode writtenClass(Class<?> type) {
        if (type == Void.class) {
            return SchemaNodes.typed(NULL);
        }
        if (type == boolean.class || type == Boolean.class) {
            return SchemaNodes.typed(BOOLEAN);
        }
        if (INTEGERS.contains(type)) {
            return SchemaNodes.typed(INTEGER);
        }
        if (type == char.class || type == Character.class) {
            return SchemaNodes.typed(STRING);
        }
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            return SchemaNodes.typed(NUMBER);
        }
        if (type == byte[].class) {
            ObjectNode bytes = SchemaNodes.typed(STRING);
            bytes.put(CONTENT_ENCODING, BASE64);
            return bytes;
        }
        if (Map.class.isAssignableFrom(type) || Optional.class.equals(type) || Collection.class.isAssignableFrom(type) || type.isArray()) {
            throw new UnschemableTypeException(type.getTypeName(), NOT_JSON);
        }
        if (type.isEnum()) {
            return definitions.define(type, () -> enumeration(type));
        }
        if (type.isSealed()) {
            return definitions.define(type, () -> alternatives(type));
        }
        if (type.isRecord()) {
            return writtenRecord(type);
        }
        if (Path.class.equals(type) || !type.isInterface() && Modifier.isFinal(type.getModifiers())) {
            return SchemaNodes.typed(STRING);
        }
        throw new UnschemableTypeException(type.getTypeName(), NOT_WRITTEN_ONE_WAY);
    }

    private JsonNode writtenRecord(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        boolean variant = ViewShape.isVariantOfASealedType(type);
        if (components.length == 1 && !variant) {
            return unwrapped(type, components[0]);
        }
        return definitions.define(type, () -> {
            ObjectNode properties = SidecarJson.object();
            if (variant) {
                properties.set(ViewShape.KIND, SchemaNodes.constant(ViewShape.kindOf(type)));
            }
            Arrays.stream(components).forEach(component -> properties.set(component.getName(), written(component.getGenericType())));
            return SchemaNodes.closedObject(properties);
        });
    }

    private JsonNode unwrapped(Class<?> type, RecordComponent component) {
        if (!unwrapping.add(type)) {
            throw new UnschemableTypeException(type.getTypeName(), HOLDS_ITSELF);
        }
        JsonNode node = written(component.getGenericType());
        unwrapping.remove(type);
        return node;
    }

    private ObjectNode alternatives(Class<?> sealed) {
        ObjectNode node = SidecarJson.object();
        node.set(ONE_OF, SidecarJson.array(Arrays.stream(sealed.getPermittedSubclasses()).map(this::variant).toList()));
        node.putObject(DISCRIMINATOR).put(PROPERTY_NAME, ViewShape.KIND);
        return node;
    }

    private JsonNode variant(Class<?> type) {
        if (!type.isRecord() && !type.isSealed()) {
            throw new UnschemableTypeException(type.getTypeName(), NO_KIND);
        }
        return writtenClass(type);
    }

    private static ObjectNode enumeration(Class<?> type) {
        ObjectNode node = SchemaNodes.typed(STRING);
        node.set(ENUM, SidecarJson.texts(Arrays.stream(type.getEnumConstants()).map(constant -> ((Enum<?>) constant).name()).toList()));
        return node;
    }
}

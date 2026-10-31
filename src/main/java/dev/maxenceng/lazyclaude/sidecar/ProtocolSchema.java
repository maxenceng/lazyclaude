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
import java.util.TreeMap;
import java.util.function.Supplier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The JSON Schema of the protocol, written from the Payloads and Views by the rules {@link SidecarJson} writes them with (ADR 0073).
 * Results and notification params are written; params are read by Jackson into the Payload's components, so a Payload is an object of
 * all of them. The writer looks at a value's runtime class and this at a declared type, so a declared type whose values could be
 * written another way than it states (an interface, an abstract class, an array other than bytes) is refused rather than guessed.
 */
final class ProtocolSchema {

    private static final String SCHEMA_URI = "https://json-schema.org/draft/2020-12/schema";
    private static final String TITLE = "LazyclaudeProtocol";
    private static final String DESCRIPTION =
            "Every method and notification of the lazyclaude sidecar, generated from its Payloads and Views.";
    private static final String SCHEMA = "$schema";
    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String DEFS = "$defs";
    private static final String REF = "$ref";
    private static final String REF_PREFIX = "#/$defs/";
    private static final String TYPE = "type";
    private static final String OBJECT = "object";
    private static final String STRING = "string";
    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    private static final String BOOLEAN = "boolean";
    private static final String NULL = "null";
    private static final String ARRAY = "array";
    private static final String ITEMS = "items";
    private static final String ANY_OF = "anyOf";
    private static final String ONE_OF = "oneOf";
    private static final String CONST = "const";
    private static final String ENUM = "enum";
    private static final String CONTENT_ENCODING = "contentEncoding";
    private static final String BASE64 = "base64";
    private static final String DISCRIMINATOR = "discriminator";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    private static final String METHODS = "methods";
    private static final String INPUTS = "inputs";
    private static final String NOTIFICATIONS = "notifications";
    private static final String PARAMS = "params";
    private static final String RESULT = "result";
    private static final Set<Class<?>> INTEGERS =
            Set.of(int.class, long.class, short.class, byte.class, Integer.class, Long.class, Short.class, Byte.class);
    private static final String CANNOT_STATE = "the protocol cannot state %s: %s";
    private static final String NOT_JSON = "its shape has to change before it is written as JSON";
    private static final String NO_SEALED_PARAMS = "a Payload component that is or holds a sealed type is not read yet";
    private static final String NOT_WRITTEN_ONE_WAY = "its values are written by their own class, which it does not name";
    private static final String NO_KIND = "a variant that is not a record or a sealed type carries no kind";
    private static final String HOLDS_ITSELF = "a record of one component is written as that component, so it cannot hold itself";
    private static final String OF = "%s of %s";
    private static final String SAME_NAME = "%s and %s share the name %s, which keys the schema's definitions";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final String ROOT = ProtocolSchema.class.getPackageName().substring(0, ProtocolSchema.class.getPackageName().lastIndexOf(PACKAGE_SEPARATOR));
    private static final String NO_MODULE = "it is in no module of " + ROOT + ", which qualifies the schema's definitions";
    private static final String MODULE_PREFIX = ROOT + PACKAGE_SEPARATOR;
    private static final String QUALIFIED = "%s.%s";

    private final Map<String, Class<?>> owners = new TreeMap<>();
    private final Map<String, JsonNode> definitions = new TreeMap<>();
    private final Set<Class<?>> unwrapping = new HashSet<>();

    private ProtocolSchema() {}

    static String text(
            List<ProtocolMethod<?, ?>> methods, List<ProtocolInput<?>> inputs, List<ProtocolNotification<?>> notifications) {
        return SidecarJson.pretty(new ProtocolSchema().root(methods, inputs, notifications));
    }

    private ObjectNode root(
            List<ProtocolMethod<?, ?>> methods, List<ProtocolInput<?>> inputs, List<ProtocolNotification<?>> notifications) {
        ObjectNode methodProperties = SidecarJson.object();
        methods.forEach(method -> methodProperties.set(method.name(), call(method)));
        ObjectNode inputProperties = SidecarJson.object();
        inputs.forEach(input -> inputProperties.set(input.name(), taken(input)));
        ObjectNode notificationProperties = SidecarJson.object();
        notificationProperties.set(JsonRpcMessages.readyName(), ready());
        notifications.forEach(notification -> notificationProperties.set(notification.name(), push(notification)));
        ObjectNode root = SidecarJson.object();
        root.put(SCHEMA, SCHEMA_URI);
        root.put(TITLE_KEY, TITLE);
        root.put(DESCRIPTION_KEY, DESCRIPTION);
        root.put(TYPE, OBJECT);
        ObjectNode properties = root.putObject(PROPERTIES);
        properties.set(METHODS, closedObject(methodProperties));
        properties.set(INPUTS, closedObject(inputProperties));
        properties.set(NOTIFICATIONS, closedObject(notificationProperties));
        root.set(REQUIRED, names(properties));
        root.put(ADDITIONAL_PROPERTIES, false);
        ObjectNode defs = root.putObject(DEFS);
        definitions.forEach(defs::set);
        return root;
    }

    private ObjectNode call(ProtocolMethod<?, ?> method) {
        ObjectNode properties = SidecarJson.object();
        properties.set(PARAMS, definePayload(method.params()));
        properties.set(RESULT, written(method.result()));
        return closedObject(properties);
    }

    private ObjectNode taken(ProtocolInput<?> input) {
        ObjectNode properties = SidecarJson.object();
        properties.set(PARAMS, definePayload(input.params()));
        return closedObject(properties);
    }

    private ObjectNode push(ProtocolNotification<?> notification) {
        ObjectNode properties = SidecarJson.object();
        properties.set(PARAMS, written(notification.params()));
        return closedObject(properties);
    }

    private static ObjectNode ready() {
        ObjectNode version = SidecarJson.object();
        version.put(CONST, JsonRpcMessages.protocolVersion());
        ObjectNode properties = SidecarJson.object();
        properties.set(JsonRpcMessages.protocolField(), version);
        ObjectNode params = SidecarJson.object();
        params.set(PARAMS, closedObject(properties));
        return closedObject(params);
    }

    private static ObjectNode closedObject(ObjectNode properties) {
        ObjectNode object = SidecarJson.object();
        object.put(TYPE, OBJECT);
        object.set(PROPERTIES, properties);
        object.set(REQUIRED, names(properties));
        object.put(ADDITIONAL_PROPERTIES, false);
        return object;
    }

    private static ArrayNode names(ObjectNode properties) {
        return SidecarJson.texts(List.copyOf(properties.propertyNames()));
    }

    private JsonNode definePayload(Class<?> payload) {
        return define(payload, () -> {
            ObjectNode properties = SidecarJson.object();
            Arrays.stream(payload.getRecordComponents()).forEach(component -> properties.set(component.getName(), read(component)));
            return closedObject(properties);
        });
    }

    private JsonNode read(RecordComponent component) {
        if (holdsASealedType(component.getGenericType())) {
            // deferred: a Payload cannot take a sealed type — close it when a method takes one, with a test of how Jackson reads it.
            throw new UnschemableTypeException(CANNOT_STATE.formatted(OF.formatted(component.getName(), component.getDeclaringRecord().getSimpleName()), NO_SEALED_PARAMS));
        }
        return written(component.getGenericType());
    }

    private static boolean holdsASealedType(Type type) {
        return switch (type) {
            case ParameterizedType generic -> Arrays.stream(generic.getActualTypeArguments()).anyMatch(ProtocolSchema::holdsASealedType);
            case Class<?> plain -> plain.isSealed()
                    || plain.isRecord() && Arrays.stream(plain.getRecordComponents()).anyMatch(held -> holdsASealedType(held.getGenericType()));
            default -> false;
        };
    }

    private JsonNode written(Type type) {
        return switch (type) {
            case ParameterizedType generic -> writtenGeneric(generic);
            case Class<?> plain -> writtenClass(plain);
            default -> throw new UnschemableTypeException(CANNOT_STATE.formatted(type.getTypeName(), NOT_JSON));
        };
    }

    private JsonNode writtenGeneric(ParameterizedType generic) {
        Class<?> raw = (Class<?>) generic.getRawType();
        Type argument = generic.getActualTypeArguments()[0];
        if (Optional.class.equals(raw)) {
            ObjectNode node = SidecarJson.object();
            node.set(ANY_OF, SidecarJson.array(List.of(written(argument), typed(NULL))));
            return node;
        }
        if (Collection.class.isAssignableFrom(raw)) {
            ObjectNode node = typed(ARRAY);
            node.set(ITEMS, written(argument));
            return node;
        }
        throw new UnschemableTypeException(CANNOT_STATE.formatted(generic.getTypeName(), NOT_JSON));
    }

    private JsonNode writtenClass(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return typed(BOOLEAN);
        }
        if (INTEGERS.contains(type)) {
            return typed(INTEGER);
        }
        if (type == char.class || type == Character.class) {
            return typed(STRING);
        }
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            return typed(NUMBER);
        }
        if (type == byte[].class) {
            ObjectNode bytes = typed(STRING);
            bytes.put(CONTENT_ENCODING, BASE64);
            return bytes;
        }
        if (Map.class.isAssignableFrom(type) || Optional.class.equals(type) || Collection.class.isAssignableFrom(type) || type.isArray()) {
            throw new UnschemableTypeException(CANNOT_STATE.formatted(type.getTypeName(), NOT_JSON));
        }
        if (type.isEnum()) {
            return define(type, () -> enumeration(type));
        }
        if (type.isSealed()) {
            return define(type, () -> alternatives(type));
        }
        if (type.isRecord()) {
            return writtenRecord(type);
        }
        if (Path.class.equals(type) || !type.isInterface() && Modifier.isFinal(type.getModifiers())) {
            return typed(STRING);
        }
        throw new UnschemableTypeException(CANNOT_STATE.formatted(type.getTypeName(), NOT_WRITTEN_ONE_WAY));
    }

    private JsonNode writtenRecord(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        boolean variant = ViewShape.isVariantOfASealedType(type);
        if (components.length == 1 && !variant) {
            return unwrapped(type, components[0]);
        }
        return define(type, () -> {
            ObjectNode properties = SidecarJson.object();
            if (variant) {
                properties.putObject(ViewShape.KIND).put(CONST, ViewShape.kindOf(type));
            }
            Arrays.stream(components).forEach(component -> properties.set(component.getName(), written(component.getGenericType())));
            return closedObject(properties);
        });
    }

    private JsonNode unwrapped(Class<?> type, RecordComponent component) {
        if (!unwrapping.add(type)) {
            throw new UnschemableTypeException(CANNOT_STATE.formatted(type.getTypeName(), HOLDS_ITSELF));
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
            throw new UnschemableTypeException(CANNOT_STATE.formatted(type.getTypeName(), NO_KIND));
        }
        return writtenClass(type);
    }

    private static ObjectNode enumeration(Class<?> type) {
        ObjectNode node = typed(STRING);
        node.set(ENUM, SidecarJson.texts(Arrays.stream(type.getEnumConstants()).map(constant -> ((Enum<?>) constant).name()).toList()));
        return node;
    }

    private static ObjectNode typed(String type) {
        ObjectNode node = SidecarJson.object();
        node.put(TYPE, type);
        return node;
    }

    private JsonNode define(Class<?> type, Supplier<JsonNode> definition) {
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

    /** The direct subpackage of the root a type is in, so two contexts may own a type of one name (ADR 0073). */
    private static String moduleOf(Class<?> type) {
        if (!type.getPackageName().startsWith(MODULE_PREFIX)) {
            throw new UnschemableTypeException(CANNOT_STATE.formatted(type.getTypeName(), NO_MODULE));
        }
        String below = type.getPackageName().substring(MODULE_PREFIX.length());
        int end = below.indexOf(PACKAGE_SEPARATOR);
        return end < 0 ? below : below.substring(0, end);
    }
}

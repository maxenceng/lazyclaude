package dev.maxenceng.lazyclaude.sidecar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The one place Jackson is used for the protocol: it parses lines and params, and writes the Views the contexts answer with. Views and
 * Payloads carry no annotation, so how they are written is decided here (ADR 0050): a record that is one of a sealed type's variants
 * is an object led by its {@code kind}, a record of one component is that component, any other record is an object of its
 * components, and anything else (a path, an instant, an enum constant) is its text.
 */
final class SidecarJson {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .build();
    private static final String KIND = "kind";
    private static final String VIEW = "View";

    private SidecarJson() {}

    static JsonNode parse(String line) {
        return MAPPER.readTree(line);
    }

    static String line(JsonNode message) {
        return MAPPER.writeValueAsString(message);
    }

    static ObjectNode object() {
        return MAPPER.createObjectNode();
    }

    /** @throws JsonRpcFaultException when {@code params} are not a {@code type}, or leave out a component of it */
    static <T> T params(JsonNode params, Class<T> type) {
        try {
            return MAPPER.treeToValue(params, type);
        } catch (JacksonException e) {
            throw new JsonRpcFaultException(ErrorCode.INVALID_PARAMS, "invalid params: " + e.getOriginalMessage(), e);
        }
    }

    static JsonNode tree(Object value) {
        return switch (value) {
            case null -> NullNode.getInstance();
            case Boolean flag -> MAPPER.getNodeFactory().booleanNode(flag);
            case Number number -> MAPPER.valueToTree(number);
            case Optional<?> optional -> optional.map(SidecarJson::tree).orElse(NullNode.getInstance());
            case Collection<?> items -> array(items);
            case Record record -> fromRecord(record);
            default -> MAPPER.getNodeFactory().stringNode(value.toString());
        };
    }

    private static ArrayNode array(Collection<?> items) {
        ArrayNode array = MAPPER.createArrayNode();
        items.stream().map(SidecarJson::tree).forEach(array::add);
        return array;
    }

    private static JsonNode fromRecord(Record record) {
        RecordComponent[] components = record.getClass().getRecordComponents();
        boolean variant = isVariantOfASealedType(record.getClass());
        if (components.length == 1 && !variant) {
            return tree(valueOf(record, components[0]));
        }
        ObjectNode node = MAPPER.createObjectNode();
        if (variant) {
            node.put(KIND, kindOf(record.getClass()));
        }
        Arrays.stream(components).forEach(component -> node.set(component.getName(), tree(valueOf(record, component))));
        return node;
    }

    private static boolean isVariantOfASealedType(Class<?> type) {
        return Stream.concat(Stream.of(type.getInterfaces()), Stream.ofNullable(type.getSuperclass())).anyMatch(Class::isSealed);
    }

    /** The simple name without {@code View}, in lower camel case: {@code UnavailableBoardView} is {@code unavailableBoard}. */
    static String kindOf(Class<?> type) {
        String name = type.getSimpleName();
        String bare = name.endsWith(VIEW) && name.length() > VIEW.length() ? name.substring(0, name.length() - VIEW.length()) : name;
        return bare.substring(0, 1).toLowerCase(Locale.ROOT) + bare.substring(1);
    }

    private static Object valueOf(Record record, RecordComponent component) {
        try {
            return component.getAccessor().invoke(record);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ViewUnreadableException("cannot read %s of %s".formatted(component.getName(), record.getClass().getName()), e);
        }
    }
}

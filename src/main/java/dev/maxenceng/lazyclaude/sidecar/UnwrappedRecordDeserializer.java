package dev.maxenceng.lazyclaude.sidecar;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;

/**
 * Reads a record of one component from that component, as the writer writes it and the schema states it: {@code SessionId} from its
 * text, {@code PaneWidth} from its number (ADR 0050). A value the record refuses is the caller's invalid param. A method's own params
 * are an object of the record's component, which the default reader reads, so an object is left to it unless the component is one.
 */
final class UnwrappedRecordDeserializer extends DelegatingDeserializer {

    private static final String CANNOT_BUILD = "cannot build %s";

    private final Constructor<?> canonical;
    private final RecordComponent component;

    UnwrappedRecordDeserializer(Class<?> type, ValueDeserializer<?> asObject) {
        super(asObject);
        this.component = type.getRecordComponents()[0];
        try {
            this.canonical = type.getDeclaredConstructor(component.getType());
        } catch (NoSuchMethodException e) {
            throw new ViewUnreadableException(CANNOT_BUILD.formatted(type.getName()), e);
        }
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> asObject) {
        return new UnwrappedRecordDeserializer(handledType(), asObject);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.hasToken(JsonToken.START_OBJECT) && !ViewShape.isWrittenAsObject(component.getType())) {
            return super.deserialize(parser, context);
        }
        JavaType componentType = context.constructType(component.getGenericType());
        Object value = context.readValue(parser, componentType);
        try {
            return canonical.newInstance(value);
        } catch (InvocationTargetException refused) {
            return context.handleInstantiationProblem(handledType(), value, refused.getCause());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ViewUnreadableException(CANNOT_BUILD.formatted(handledType().getName()), e);
        }
    }
}

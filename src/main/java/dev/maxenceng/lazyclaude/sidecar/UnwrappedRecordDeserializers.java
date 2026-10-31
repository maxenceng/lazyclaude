package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;

/** Gives every record of one component that is not a sealed type's variant the deserializer that reads it from its component. */
final class UnwrappedRecordDeserializers extends ValueDeserializerModifier {

    @Override
    public ValueDeserializer<?> modifyDeserializer(
            DeserializationConfig config, BeanDescription.Supplier description, ValueDeserializer<?> deserializer) {
        Class<?> type = description.getBeanClass();
        return type.isRecord() && ViewShape.isUnwrapped(type) ? new UnwrappedRecordDeserializer(type, deserializer) : deserializer;
    }
}

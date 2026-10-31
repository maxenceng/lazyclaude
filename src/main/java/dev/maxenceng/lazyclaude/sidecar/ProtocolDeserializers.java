package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;

/**
 * Gives every sealed type the deserializer that reads it by its kind, every variant of one the deserializer that checks its own kind,
 * and every record of one component that is not a variant the deserializer that reads it from its component.
 */
final class ProtocolDeserializers extends ValueDeserializerModifier {

    @Override
    public ValueDeserializer<?> modifyDeserializer(
            DeserializationConfig config, BeanDescription.Supplier description, ValueDeserializer<?> deserializer) {
        Class<?> type = description.getBeanClass();
        if (type.isSealed() && !type.isRecord()) {
            return new SealedViewDeserializer(type);
        }
        if (type.isRecord() && ViewShape.isVariantOfASealedType(type)) {
            return new VariantRecordDeserializer(type, deserializer);
        }
        return type.isRecord() && ViewShape.isUnwrapped(type) ? new UnwrappedRecordDeserializer(type, deserializer) : deserializer;
    }
}

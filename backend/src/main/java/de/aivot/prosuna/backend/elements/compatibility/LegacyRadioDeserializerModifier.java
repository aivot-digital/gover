package de.aivot.prosuna.backend.elements.compatibility;

import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.node.ObjectNode;

/** Accepts the retired radio type only at the element deserialization boundary. */
public final class LegacyRadioDeserializerModifier extends ValueDeserializerModifier {
    // This persisted type id must never be assigned to another element.
    public static final String LEGACY_RADIO_TYPE_ID = "11";

    @Override
    @Nonnull
    public ValueDeserializer<?> modifyDeserializer(@Nonnull DeserializationConfig config,
                                                    @Nonnull BeanDescription.Supplier bean,
                                                    @Nonnull ValueDeserializer<?> deserializer) {
        return bean.getBeanClass() == SelectInputElement.class
                ? new RadioCompatibilityDeserializer(deserializer) : deserializer;
    }

    private static final class RadioCompatibilityDeserializer extends DelegatingDeserializer {
        private RadioCompatibilityDeserializer(@Nonnull ValueDeserializer<?> delegate) {
            super(delegate);
        }

        @Override
        @Nonnull
        protected ValueDeserializer<?> newDelegatingInstance(@Nonnull ValueDeserializer<?> delegate) {
            return new RadioCompatibilityDeserializer(delegate);
        }

        @Override
        @Nullable
        public Object deserialize(@Nonnull JsonParser parser, @Nonnull DeserializationContext context) {
            var node = context.readTree(parser);
            if (node instanceof ObjectNode object && LEGACY_RADIO_TYPE_ID.equals(object.path("type").asText())) {
                object.put("type", ElementType.Select.getKey());
                object.put("presentation", SelectInputPresentation.Radio.getKey());
            }
            try (var normalized = node.traverse(context)) {
                normalized.nextToken();
                return _delegatee.deserialize(normalized, context);
            }
        }
    }
}

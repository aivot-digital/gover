package de.aivot.prosuna.backend.ai.tools;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.jackson.JsonSubTypesResolver;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

final class AiElementSchemaGenerator {
    private AiElementSchemaGenerator() {
    }

    @Nonnull
    static ObjectNode generateSchema(@Nonnull JsonMapper jsonMapper, @Nonnull ElementType requestedType)
            throws ElementDataConversionException {
        var elementClass = ElementType.getElementClass(requestedType).getClass();
        var elementTypes = supportedElementTypes();
        var config = new SchemaGeneratorConfigBuilder(jsonMapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);

        // Use the mapper's actual enum values, including numeric @JsonValue values.
        config.forTypesInGeneral().withCustomDefinitionProvider((type, context) -> {
            var constants = type.getErasedType().getEnumConstants();
            if (constants == null) {
                return null;
            }
            var schema = jsonMapper.createObjectNode();
            schema.set("enum", jsonMapper.valueToTree(constants));
            return new CustomDefinition(schema);
        });
        config.forTypesInGeneral().withSubtypeResolver((type, context) -> {
            var rawType = type.getErasedType();
            if (!BaseElement.class.isAssignableFrom(rawType) || elementTypes.containsKey(rawType)) {
                return null;
            }
            // Intermediate classes such as BaseFormElement inherit Jackson's subtype
            // registration, but only assignable classes are valid children here.
            return elementTypes.keySet().stream()
                    .filter(rawType::isAssignableFrom)
                    .map(context.getTypeContext()::resolve)
                    .toList();
        });
        config.with(new JacksonSchemaModule(
                JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                JacksonOption.IGNORE_TYPE_INFO_TRANSFORM
        ));
        config.with(new Swagger2Module());
        config.with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES);

        // Jackson's default schema transformation uses string discriminators. Element
        // discriminators are numeric; retain the transformation for other models.
        config.forTypesInGeneral().withCustomDefinitionProvider(new JsonSubTypesResolver() {
            @Override
            @Nullable
            public CustomDefinition provideCustomSchemaDefinition(@Nonnull ResolvedType type, @Nonnull SchemaGenerationContext context) {
                return BaseElement.class.isAssignableFrom(type.getErasedType())
                        ? null : super.provideCustomSchemaDefinition(type, context);
            }
        });
        config.forFields().withCustomDefinitionProvider((field, context) -> {
            if (field.getDeclaredType().getErasedType() != ElementType.class || !field.getDeclaredName().equals("type")) {
                return null;
            }
            var elementType = elementTypes.get(field.getDeclarationDetails().getSchemaTargetType().getErasedType());
            if (elementType == null) {
                return null;
            }
            return new CustomPropertyDefinition(jsonMapper.createObjectNode()
                    .put("type", "integer")
                    .put("const", elementType.getKey()));
        });
        config.forFields().withRequiredCheck(field ->
                field.getDeclaringType().getErasedType() == BaseElement.class && field.getDeclaredName().equals("type"));
        // Older models have unannotated reference fields that are serialized as null.
        // Container entries do not inherit the nullability of the container itself.
        config.forFields().withNullableCheck(field -> !field.isFakeContainerItemScope()
                && !field.getType().getErasedType().isPrimitive()
                && (field.getAnnotationConsideringFieldAndGetter(Nullable.class) != null
                    || field.getAnnotationConsideringFieldAndGetter(Nonnull.class) == null));

        var generator = new SchemaGenerator(config.build());
        return generator.generateSchema(elementClass);
    }

    @Nonnull
    private static Map<Class<? extends BaseElement>, ElementType> supportedElementTypes() {
        var types = new LinkedHashMap<Class<? extends BaseElement>, ElementType>();
        for (var type : ElementType.values()) {
            if (type == ElementType.SubmittedStep) {
                continue;
            }
            try {
                types.put(ElementType.getElementClass(type).getClass(), type);
            } catch (ElementDataConversionException exception) {
                throw new IllegalStateException("Das Modell für den folgenden Formularelementtyp konnte nicht ermittelt werden: " + type, exception);
            }
        }
        return types;
    }
}

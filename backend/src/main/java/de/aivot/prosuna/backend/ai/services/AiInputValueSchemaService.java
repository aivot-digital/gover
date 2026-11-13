package de.aivot.prosuna.backend.ai.services;

import com.fasterxml.classmate.TypeResolver;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.InputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DateInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DateRangeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.enums.DateType;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.*;
import java.time.temporal.TemporalAccessor;

@Service
public class AiInputValueSchemaService {
    private final JsonMapper mapper;

    public AiInputValueSchemaService(@Nonnull JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Nonnull
    public ObjectNode forType(int typeKey) {
        var type = ElementType.findElement(typeKey)
                .orElseThrow(() -> new IllegalArgumentException("Der Elementtyp wird nicht unterstützt."));
        try {
            return forElement(ElementType.getElementClass(type));
        } catch (de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException exception) {
            throw new IllegalArgumentException("Der Elementtyp wird nicht unterstützt.");
        }
    }

    @Nonnull
    public ObjectNode forElement(@Nonnull BaseElement element) {
        var inputType = mapper.constructType(element.getClass()).findSuperType(InputElement.class);
        if (inputType == null) {
            throw new IllegalArgumentException("Dieses Element ist kein Eingabefeld.");
        }
        var config = new SchemaGeneratorConfigBuilder(mapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonSchemaModule()).with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES);
        config.forFields().withNullableCheck(field -> !field.isFakeContainerItemScope()
                && field.getAnnotationConsideringFieldAndGetter(jakarta.annotation.Nullable.class) != null);
        config.forTypesInGeneral().withCustomDefinitionProvider((type, context) -> {
            var raw = type.getErasedType();
            if (raw.isEnum()) {
                var schema = mapper.createObjectNode();
                schema.set("enum", mapper.valueToTree(raw.getEnumConstants()));
                return new CustomDefinition(schema);
            }
            if (TemporalAccessor.class.isAssignableFrom(raw)) {
                var schema = mapper.createObjectNode().put("type", "string");
                if (raw == LocalTime.class) {
                    schema.put("pattern", "^\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?$");
                } else if (raw == Instant.class || raw == OffsetDateTime.class) {
                    schema.put("format", "date-time");
                } else {
                    var mode = element instanceof DateInputElement date ? date.getMode()
                            : element instanceof DateRangeInputElement range ? range.getMode() : DateType.Day;
                    if (mode == DateType.Year) schema.put("pattern", "^\\d{4}$");
                    else if (mode == DateType.Month) schema.put("pattern", "^\\d{4}-\\d{2}$");
                    else schema.put("format", "date");
                }
                return new CustomDefinition(schema);
            }
            if (BaseElement.class.isAssignableFrom(raw)) {
                // UI definitions are edited incrementally; expanding every subtype would dominate the context.
                var schema = mapper.createObjectNode().put("type", "object");
                var properties = schema.putObject("properties");
                properties.putObject("id").put("type", "string");
                var typeSchema = properties.putObject("type").put("type", "integer");
                if (element instanceof UiDefinitionInputElement ui && ui.getElementType() != null) {
                    typeSchema.put("const", ui.getElementType().getKey());
                }
                schema.putArray("required").add("id").add("type");
                schema.put("description", "Elementeigenschaften gezielt nachschlagen; eingebettete Formulare mit bearbeite-knotenformular bearbeiten.");
                return new CustomDefinition(schema);
            }
            return null;
        });
        var schema = new SchemaGenerator(config.build()).generateSchema(resolve(inputType.containedType(0)));
        if (element instanceof de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElement table && table.getFields() != null) {
            var properties = schema.putObject("items").put("type", "object").putObject("properties");
            for (var field : table.getFields()) {
                if (field.getKey() != null && field.getDatatype() != null) {
                    properties.putObject(field.getKey()).putArray("type").add(field.getDatatype().getKey()).add("null");
                }
            }
        }
        return schema;
    }

    private com.fasterxml.classmate.ResolvedType resolve(JavaType type) {
        var resolver = new TypeResolver();
        if (type.isArrayType()) return resolver.arrayType(resolve(type.getContentType()));
        var parameters = new com.fasterxml.classmate.ResolvedType[type.containedTypeCount()];
        for (int i = 0; i < parameters.length; i++) parameters[i] = resolve(type.containedType(i));
        return resolver.resolve(type.getRawClass(), parameters);
    }
}

package de.aivot.prosuna.backend.ai.tools;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiChatElementPropertySchemaTest {
    private final JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final ToolCallback tool = Arrays.stream(ToolCallbacks.from(new AiChatElementTools(mapper,
                    mock(AiUiElementChatSessionCacheRepository.class))))
            .filter(callback -> callback.getToolDefinition().name().equals("hole-json-schema-fuer-element-eigenschaft"))
            .findFirst().orElseThrow();

    @Test
    void describesInheritedPropertiesAndUsesExactJsonNames() {
        var label = schema("label");
        assertThat(label.path("$schema").asString()).isEqualTo("https://json-schema.org/draft/2020-12/schema");
        assertThat(label.path("type").asString()).isEqualTo("string");
        assertThat(label.has("properties")).isFalse();
        assertThat(schema("isMultiline").path("type").asString()).isEqualTo("boolean");
    }

    @Test
    void preservesGenericListItemTypes() {
        var schema = schema("suggestions");
        assertThat(schema.path("type").asString()).isEqualTo("array");
        assertThat(schema.path("items").path("type").asString()).isEqualTo("string");
        var validator = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
        assertThat(validator.validate(mapper.readTree("[\"one\",\"two\"]"))).isEmpty();
        assertThat(validator.validate(mapper.readTree("[42]"))).isNotEmpty();
    }

    @Test
    void describesNestedValuesWithoutTheContainingElement() {
        var schema = schema("pattern");
        assertThat(schema.path("properties").has("regex")).isTrue();
        assertThat(schema.path("properties").has("message")).isTrue();
        assertThat(schema.toString()).doesNotContain("visibility", "isMultiline", "children");
        assertReferencesResolve(schema, schema);
        var validator = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
        assertThat(validator.validate(mapper.readTree("{\"regex\":\"[a-z]+\",\"message\":\"Example\"}"))).isEmpty();
        assertThat(validator.validate(mapper.readTree("{\"regex\":42}"))).isNotEmpty();
    }

    @Test
    void rejectsUnsupportedTypesAndUnknownOrNestedPropertyNames() {
        for (var typeKey : new int[]{-1, ElementType.SubmittedStep.getKey()}) {
            assertThat(call(typeKey, "label")).contains("Der Formularelementtyp wird nicht unterstützt.");
        }
        for (var property : new String[]{"unknown", "", "multiline", "pattern.regex", "/pattern/regex"}) {
            assertThat(call(ElementType.Text.getKey(), property))
                    .contains("Die Eigenschaft ist für diesen Formularelementtyp nicht verfügbar.");
        }
    }

    private JsonNode schema(String property) {
        return mapper.readTree(call(ElementType.Text.getKey(), property));
    }

    private String call(int typeKey, String property) {
        return tool.call(mapper.writeValueAsString(mapper.createObjectNode()
                .put("typeKey", typeKey).put("propertyName", property)));
    }

    private void assertReferencesResolve(JsonNode schema, JsonNode node) {
        if (node.has("$ref")) {
            var reference = node.path("$ref").asString();
            assertThat(reference).startsWith("#/");
            assertThat(schema.at(reference.substring(1)).isMissingNode()).isFalse();
        }
        node.forEach(child -> assertReferencesResolve(schema, child));
    }
}

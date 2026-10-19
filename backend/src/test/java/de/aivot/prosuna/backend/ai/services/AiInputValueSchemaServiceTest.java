package de.aivot.prosuna.backend.ai.services;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.InputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.enums.DateType;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class AiInputValueSchemaServiceTest {
    private final tools.jackson.databind.json.JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final AiInputValueSchemaService schemas = new AiInputValueSchemaService(mapper);

    @ParameterizedTest
    @EnumSource(value = ElementType.class, names = "SubmittedStep", mode = EnumSource.Mode.EXCLUDE)
    void describesEveryInputAndRejectsNonInputs(ElementType type) throws Exception {
        var element = ElementType.getElementClass(type);
        if (!(element instanceof InputElement<?>)) {
            assertThatThrownBy(() -> schemas.forType(type.getKey())).isInstanceOf(IllegalArgumentException.class);
            return;
        }
        var schema = schemas.forType(type.getKey());
        assertThat(schema.toString()).doesNotContain("ElementValueFunctions");
        assertThat(schema.toString().length()).isLessThan(25000);
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
    }

    @Test
    void usesLiteralTypesAndRetainsListItemTypes() {
        assertThat(schemas.forType(15).path("type").asString()).isEqualTo("string");
        assertThat(schemas.forType(4).path("type").asString()).isEqualTo("boolean");
        var chip = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemas.forType(32));
        assertThat(chip.validate(mapper.readTree("[\"a\"]"))).isEmpty();
        assertThat(chip.validate(mapper.readTree("[5]"))).isNotEmpty();
    }

    @Test
    void describesTemporalWireValuesAndConstrainedUiRootWithoutExpansion() {
        var year = new DateInputElement().setMode(DateType.Year);
        assertThat(schemas.forElement(year).path("pattern").asString()).isEqualTo("^\\d{4}$");
        assertThat(schemas.forType(33).path("type").asString()).isEqualTo("string");
        var ui = new UiDefinitionInputElement().setElementType(ElementType.FormLayout);
        var schema = schemas.forElement(ui);
        assertThat(schema.path("properties").path("type").path("const").asInt()).isZero();
        assertThat(schema.toString().length()).isLessThan(700);
    }
}

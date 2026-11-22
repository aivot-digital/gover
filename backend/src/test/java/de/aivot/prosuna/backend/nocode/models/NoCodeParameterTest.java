package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeParameterTest {
    @Test
    void parametersFromPluginsWithThePreviousMetadataConstructorShouldRemainOrdinaryOptions() {
        var option = new NoCodeParameterOption("Bezeichnung", "value");
        var constraints = new NoCodeNumberConstraints(true, 0.0, 10.0);
        var parameter = new NoCodeParameter(
                NoCodeDataType.String, "Einheit", "Eine Einheit.", "in", null, constraints, 0, option
        );

        assertFalse(parameter.optionsAsSentencePart());
        assertEquals(constraints, parameter.numberConstraints());
        assertEquals(0, parameter.quantityParameterIndex());
        assertEquals(option, parameter.options()[0]);

        var presented = parameter.withOptionsAsSentencePart().withSentence("").withNumberConstraints(constraints).withQuantityParameterIndex(1);
        assertTrue(presented.optionsAsSentencePart());
        assertEquals(option, presented.options()[0]);
        assertEquals(1, presented.quantityParameterIndex());
    }

    @Test
    void serializedParametersWithoutTheNewFlagShouldKeepTheirPreviousPresentation() {
        var mapper = JsonMapperTestUtils.createMapper();
        var parameter = new NoCodeParameter(
                NoCodeDataType.String, "Status", "Ein Status.", new NoCodeParameterOption("Offen", "open")
        );
        var serialized = (ObjectNode) mapper.valueToTree(parameter);
        serialized.remove("optionsAsSentencePart");

        var restored = mapper.treeToValue(serialized, NoCodeParameter.class);

        assertEquals(mapper.valueToTree(parameter), mapper.valueToTree(restored));
        assertFalse(restored.optionsAsSentencePart());
    }

    @Test
    void existingNumberConstraintsStillRequireAValue() {
        var mapper = JsonMapperTestUtils.createMapper();
        var constraints = mapper.readValue("{\"integerOnly\":true,\"minimum\":0,\"maximum\":10}", NoCodeNumberConstraints.class);
        assertEquals(new NoCodeNumberConstraints(true, 0.0, 10.0), constraints);
        assertFalse(constraints.allowEmpty());
    }
}

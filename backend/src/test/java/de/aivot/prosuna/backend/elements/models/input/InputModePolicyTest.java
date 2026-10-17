package de.aivot.prosuna.backend.elements.models.input;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputModePolicyTest {
    @Test
    void deserialize_ShouldAllowAllVariableSourcesWhenOmitted() throws Exception {
        var policy = JsonMapperTestUtils.createMapper().readValue(
                """
                        {
                          "allowedModes": ["Literal", "Variable", "NoCode", "LowCode"],
                          "defaultMode": "Literal"
                        }
                        """,
                InputModePolicy.class
        );

        assertEquals(List.of(InputVariableSource.values()), policy.allowedVariableSources());
    }

    @Test
    void deserialize_ShouldUseNoVariableSourcesWhenVariableModeIsNotAllowed() throws Exception {
        var policy = JsonMapperTestUtils.createMapper().readValue(
                """
                        {
                          "allowedModes": ["Literal", "LowCode"],
                          "defaultMode": "Literal"
                        }
                        """,
                InputModePolicy.class
        );

        assertEquals(List.of(), policy.allowedVariableSources());
    }
}

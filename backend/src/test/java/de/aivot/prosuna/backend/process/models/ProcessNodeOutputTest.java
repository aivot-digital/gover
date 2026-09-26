package de.aivot.prosuna.backend.process.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessNodeOutputTest {
    @Test
    void serialization_ShouldExposeTypeDefinition() throws Exception {
        var output = new ProcessNodeOutput(
                "result",
                "Ergebnis",
                "Das erzeugte Ergebnis.",
                "string | null"
        );
        var mapper = JsonMapper.builder().build();

        @SuppressWarnings("unchecked")
        var serializedOutput = mapper.readValue(
                mapper.writeValueAsString(output),
                Map.class
        );

        assertEquals(
                Map.of(
                        "key", "result",
                        "label", "Ergebnis",
                        "description", "Das erzeugte Ergebnis.",
                        "typeDefinition", "string | null"
                ),
                serializedOutput
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n"})
    void constructor_ShouldRejectMissingTypeDefinition(String typeDefinition) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessNodeOutput("result", "Ergebnis", "Das erzeugte Ergebnis.", typeDefinition)
        );
    }
}

package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHint;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHintType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NoCodeActionNodeV1Test {
    private NoCodeActionNodeV1 node;

    @BeforeEach
    void setUp() {
        node = new NoCodeActionNodeV1(mock(NoCodeEvaluationService.class));
    }

    @Test
    void calculateProcessDataKeyHints_ShouldAppendValidVariablePathsAndIgnoreInvalidRows() {
        var configuration = new NoCodeActionNodeV1.NoCodeActionNodeConfiguration();
        configuration.variables = Arrays.asList(
                variable("result.total"),
                variable(" result.date "),
                variable("result..invalid"),
                variable("result.total"),
                variable("items[0].name"),
                variable("items["),
                variable(" "),
                null
        );

        var hints = node.calculateProcessDataKeyHints(
                processNode(),
                configuration,
                List.of(new ProcessDataKeyHint("existing", ProcessDataKeyHintType.ProcessData))
        );

        assertEquals(
                List.of(
                        new ProcessDataKeyHint("existing", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("result.total", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("result.date", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("result.invalid", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("items[0].name", ProcessDataKeyHintType.ProcessData)
                ),
                hints
        );
    }

    private static NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration variable(String name) {
        var variable = new NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration();
        variable.name = name;
        return variable;
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(1)
                .setProcessId(2)
                .setProcessVersion(3)
                .setName("No-Code")
                .setDataKey("noCode")
                .setProcessNodeDefinitionKey("de.aivot.core.no-code")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new de.aivot.GoverBackend.elements.models.AuthoredElementValues())
                .setOutputMappings(Map.of());
    }
}

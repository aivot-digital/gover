package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataTypeValidationControlNodeV1Test {
    private final DataTypeValidationControlNodeV1 node = new DataTypeValidationControlNodeV1();

    @Test
    void shouldValidateConcreteIndicesAndNestedWildcards() throws Exception {
        var data = new ProcessExecutionData().addProcessData("matrix",
                List.of(List.of(Map.of("name", "Ada"), Map.of("name", "Grace"))));
        assertEquals("valid", execute(data, "matrix[0][1].name").getViaPort());
        assertEquals("valid", execute(data, "matrix[*][*].name").getViaPort());
    }

    @Test
    void shouldReportMissingIndicesAndWrongContainerTypes() throws Exception {
        var data = new ProcessExecutionData().addProcessData("people", List.of(Map.of("name", "Ada")));
        assertEquals("invalid", execute(data, "people[1].name").getViaPort());
        assertEquals("invalid", execute(data, "people[0].name[0]").getViaPort());
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class, () -> execute(data, "people.0.name"));
    }

    @Test
    void emptyListPassesWithoutClaimingThatAnyValuesWereChecked() throws Exception {
        var result = execute(new ProcessExecutionData().addProcessData("people", List.of()), "people[*].name");

        assertEquals("valid", result.getViaPort());
        assertEquals(true, result.getNodeData().get("isValid"));
        assertEquals(1, result.getNodeData().get("validatedRuleCount"));
        assertEquals(0, result.getNodeData().get("validatedValueCount"));
        assertEquals(List.of(), result.getNodeData().get("errors"));
        assertEquals(
                "Die Prüfung hat keine Abweichungen von den festgelegten Regeln ergeben."
                        + "\n\nAusgewertete Regeln\n:   1"
                        + "\n\nGeprüfte Werte\n:   0",
                summary(result)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void numericTextUsesTheInvalidBranchWithReadableTypeNames() throws Exception {
        var result = execute(
                new ProcessExecutionData().addProcessData("person", Map.of("age", "37")),
                "person.age",
                "number"
        );

        assertEquals("invalid", result.getViaPort());
        assertEquals(false, result.getNodeData().get("isValid"));
        assertEquals(1, result.getNodeData().get("validatedRuleCount"));
        assertEquals(1, result.getNodeData().get("validatedValueCount"));
        var errors = (List<Map<String, Object>>) result.getNodeData().get("errors");
        assertEquals(1, errors.size());
        var error = errors.getFirst();
        assertEquals(1, error.get("rowIndex"));
        assertEquals("person.age", error.get("path"));
        assertEquals("$.person.age", error.get("resolvedPath"));
        assertTrue(error.get("error").toString().contains("Erwartet: „Zahl“; vorhanden: „Text“"));

        var markdown = summary(result);
        assertTrue(markdown.startsWith("Die Prüfung hat Abweichungen von den festgelegten Regeln ergeben."), markdown);
        assertTrue(markdown.contains("person\\.age"), markdown);
        assertTrue(markdown.contains("Erwartet: „Zahl“; vorhanden: „Text“"), markdown);
        assertFalse(markdown.contains("number"), markdown);
        assertFalse(markdown.contains("string"), markdown);
    }

    @Test
    void shouldBuildExamplesForNestedArraysAndBoundPreviewAllocation() throws Exception {
        var rule = new DataTypeValidationControlNodeV1.DataTypeValidationRuleConfig();
        rule.path = "matrix[1][*].name";
        rule.expectedType = "string";
        var config = new DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig();
        config.rules = List.of(rule);
        var context = new ProcessNodeDefinitionTestingLayoutContext<>(
                new de.aivot.prosuna.backend.user.entities.UserEntity(),
                new de.aivot.prosuna.backend.process.entities.ProcessEntity(),
                new de.aivot.prosuna.backend.process.entities.ProcessVersionEntity(),
                new de.aivot.prosuna.backend.process.entities.ProcessNodeEntity(),
                new de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity(),
                config);
        var content = assertInstanceOf(
                RichTextContentElement.class,
                node.getTestingLayout(context).getChildren().getFirst()).getContent();
        var json = content.substring(content.indexOf("```json\n") + 8, content.lastIndexOf("```")).trim();
        var example = JsonMapperFactory.getInstance().readValue(json, Map.class);
        assertEquals("example", ProcessDataValueUtils
                .resolveDestinationKeyValue(example, "matrix[1][0].name"));

        rule.path = "matrix[2147483647].name";
        var limitedContent = assertInstanceOf(
                RichTextContentElement.class,
                node.getTestingLayout(context).getChildren().getFirst()).getContent();
        assertTrue(limitedContent.contains("1000 Einträge"));
    }

    private ProcessNodeExecutionResultTaskCompleted execute(ProcessExecutionData data, String path) throws Exception {
        return execute(data, path, "string");
    }

    @SuppressWarnings("unchecked")
    private ProcessNodeExecutionResultTaskCompleted execute(ProcessExecutionData data, String path, String expectedType) throws Exception {
        var rule = new DataTypeValidationControlNodeV1.DataTypeValidationRuleConfig();
        rule.path = path;
        rule.expectedType = expectedType;
        var config = new DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig();
        config.rules = List.of(rule);
        ProcessNodeExecutionInitContext<DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig> context =
                mock(ProcessNodeExecutionInitContext.class);
        when(context.getCurrentProcessExecutionData()).thenReturn(data);
        when(context.getConfigurationOfExecutingNode()).thenReturn(config);
        return assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context));
    }

    private String summary(ProcessNodeExecutionResultTaskCompleted result) {
        return node.generateExecutionSummary(new ProcessNodeExecutionSummaryContext<>(
                new DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig(),
                new ProcessNodeEntity(),
                new ProcessInstanceEntity(),
                new ProcessInstanceTaskEntity().setNodeData(result.getNodeData()),
                null, null, null, result.getViaPort()
        ));
    }
}

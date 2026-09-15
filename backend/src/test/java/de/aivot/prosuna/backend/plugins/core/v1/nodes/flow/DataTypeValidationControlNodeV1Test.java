package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
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

    @SuppressWarnings("unchecked")
    private ProcessNodeExecutionResultTaskCompleted execute(ProcessExecutionData data, String path) throws Exception {
        var rule = new DataTypeValidationControlNodeV1.DataTypeValidationRuleConfig();
        rule.path = path;
        rule.expectedType = "string";
        var config = new DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig();
        config.rules = List.of(rule);
        ProcessNodeExecutionInitContext<DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig> context =
                mock(ProcessNodeExecutionInitContext.class);
        when(context.getCurrentProcessExecutionData()).thenReturn(data);
        when(context.getConfigurationOfExecutingNode()).thenReturn(config);
        return assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context));
    }
}

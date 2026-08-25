package de.aivot.prosuna.backend.process.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.plugins.ai.v1.nodes.AiCompletionActionNodeV1;
import de.aivot.prosuna.backend.plugins.ai.v1.nodes.AiProcessDataTransformationActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.*;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.DataTypeValidationControlNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.IfFlowControlNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.terminators.DefaultTerminationNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerNodeV1;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class ProcessNodeDefinitionExecutionTypeTest {
    @ParameterizedTest
    @MethodSource("executionTypeExpectations")
    void getExecutionTypes_ShouldExposeExpectedFixedTypes(ExecutionTypeExpectation expectation) {
        ProcessNodeDefinition<?> definition = mock(expectation.definitionClass(), CALLS_REAL_METHODS);

        var executionTypes = definition.getExecutionTypes();

        assertArrayEquals(expectation.executionTypes(), executionTypes);
        assertTrue(executionTypes.length > 0);
        assertFalse(Arrays.asList(executionTypes).contains(null));
        assertEquals(executionTypes.length, Arrays.stream(executionTypes).distinct().count());

        var secondResult = definition.getExecutionTypes();
        assertNotSame(executionTypes, secondResult);

        executionTypes[0] = executionTypes[0] == ProcessNodeExecutionType.Automatic
                ? ProcessNodeExecutionType.Manual
                : ProcessNodeExecutionType.Automatic;
        assertArrayEquals(expectation.executionTypes(), definition.getExecutionTypes());
    }

    @Test
    void serialization_ShouldExposeExecutionTypesWithStableEnumValues() throws Exception {
        var mapper = JsonMapper.builder().build();
        var definition = new SerializationTestProcessNodeDefinition();

        @SuppressWarnings("unchecked")
        var serializedDefinition = mapper.readValue(
                mapper.writeValueAsString(definition),
                Map.class
        );

        assertEquals(
                List.of("Automatic", "Manual", "SemiAutomatic"),
                serializedDefinition.get("executionTypes")
        );
        assertEquals("Concise process-node abstract.", serializedDefinition.get("abstractDescription"));
        assertEquals("Detailed **process-node** description.", serializedDefinition.get("description"));
        assertEquals("https://docs.example.com/process-nodes/test", serializedDefinition.get("documentationUrl"));
        assertFalse(serializedDefinition.containsKey("abstract"));
    }

    private static Stream<ExecutionTypeExpectation> executionTypeExpectations() {
        return Stream.of(
                automatic(AiCompletionActionNodeV1.class),
                automatic(AiProcessDataTransformationActionNodeV1.class),
                manual(ApprovalActionNodeV1.class),
                automatic(CounterActionNodeV1.class),
                manual(DataChangeActionNodeV1.class),
                automatic(DataMappingActionNodeV1.class),
                mixedAutomatic(EMailActionNodeV1.class),
                automatic(HttpActionNodeV1.class),
                automatic(LowCodeActionNodeV1.class),
                manual(ManualActionNodeV1.class),
                automatic(NoCodeActionNodeV1.class),
                semiAutomatic(PaymentRequestActionNodeV1.class),
                automatic(PdfActionNodeV1.class),
                automatic(WriteExternalStorageActionNodeV1.class),
                automatic(DataTypeValidationControlNodeV1.class),
                automatic(IfFlowControlNodeV1.class),
                automatic(DefaultTerminationNodeV1.class),
                automatic(WebhookTriggerNodeV1.class),
                semiAutomatic(FormTriggerNodeV1.class)
        );
    }

    private static ExecutionTypeExpectation automatic(Class<? extends ProcessNodeDefinition<?>> definitionClass) {
        return new ExecutionTypeExpectation(
                definitionClass,
                new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic}
        );
    }

    private static ExecutionTypeExpectation mixedAutomatic(Class<? extends ProcessNodeDefinition<?>> definitionClass) {
        return new ExecutionTypeExpectation(
                definitionClass,
                new ProcessNodeExecutionType[]{
                        ProcessNodeExecutionType.Automatic,
                        ProcessNodeExecutionType.SemiAutomatic,
                }
        );
    }


    private static ExecutionTypeExpectation manual(Class<? extends ProcessNodeDefinition<?>> definitionClass) {
        return new ExecutionTypeExpectation(
                definitionClass,
                new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Manual}
        );
    }

    private static ExecutionTypeExpectation semiAutomatic(Class<? extends ProcessNodeDefinition<?>> definitionClass) {
        return new ExecutionTypeExpectation(
                definitionClass,
                new ProcessNodeExecutionType[]{ProcessNodeExecutionType.SemiAutomatic}
        );
    }

    private record ExecutionTypeExpectation(
            Class<? extends ProcessNodeDefinition<?>> definitionClass,
            ProcessNodeExecutionType[] executionTypes
    ) {
    }

    private static final class SerializationTestProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        @Nonnull
        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return "execution-types";
        }

        @Nonnull
        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Nonnull
        @Override
        public String getName() {
            return "Execution types";
        }

        @Nonnull
        @Override
        public String getAbstract() {
            return "Concise process-node abstract.";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Detailed **process-node** description.";
        }

        @Nonnull
        @Override
        public String getDocumentationUrl() {
            return "https://docs.example.com/process-nodes/test";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return ProcessNodeExecutionType.values();
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) throws ProcessNodeExecutionException {
            throw new UnsupportedOperationException("Not used in this test.");
        }

        @Nonnull
        @Override
        @JsonIgnore
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }
}

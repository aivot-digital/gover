package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.core.enums.ModuleFlags;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.plugins.form.FormPlugin;
import de.aivot.gover.backend.process.entities.ProcessEdgeEntity;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.gover.backend.process.models.ProcessNodeOutput;
import de.aivot.gover.backend.process.models.ProcessNodePort;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.repositories.ProcessRepository;
import de.aivot.gover.backend.process.repositories.ProcessVersionRepository;
import de.aivot.gover.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.gover.backend.process.services.ProcessNodeService;
import de.aivot.gover.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessNodeServiceTest {
    private static final Integer PROCESS_ID = 10;
    private static final Integer PROCESS_VERSION = 3;

    private ProcessNodeRepository processNodeRepository;
    private ProcessEdgeRepository processEdgeRepository;
    private ProcessRepository processRepository;
    private ProcessVersionRepository processVersionRepository;
    private ElementDerivationService elementDerivationService;
    private GoverConfig goverConfig;

    private ProcessNodeService service;

    @BeforeEach
    void setUp() {
        processNodeRepository = mock(ProcessNodeRepository.class);
        processEdgeRepository = mock(ProcessEdgeRepository.class);
        processRepository = mock(ProcessRepository.class);
        processVersionRepository = mock(ProcessVersionRepository.class);
        elementDerivationService = mock(ElementDerivationService.class);
        goverConfig = new GoverConfig();

        var definitionService = new ProcessNodeDefinitionService(List.of(new HintingTestNodeDefinition()));

        service = createService(
                definitionService,
                goverConfig
        );

        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(createProcess()));
        when(processVersionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, PROCESS_VERSION)))
                .thenReturn(Optional.of(createProcessVersion()));
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of());
        when(elementDerivationService.derive(any()))
                .thenReturn(new DerivedRuntimeElementData());
    }

    private ProcessNodeService createService(ProcessNodeDefinitionService definitionService,
                                             GoverConfig config) {
        return new ProcessNodeService(
                processNodeRepository,
                definitionService,
                elementDerivationService,
                mock(UserService.class),
                processRepository,
                processVersionRepository,
                processEdgeRepository,
                config
        );
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldReturnHintsWithTheirSourceNodesInDependencyOrder() throws Exception {
        var nodeA = createNode(1, "a");
        var nodeB = createNode(2, "b");
        var nodeC = createNode(3, "c");
        var targetNode = createNode(4, "target");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(targetNode, nodeC, nodeA, nodeB));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(
                        createEdge(1, nodeA.getId(), nodeB.getId()),
                        createEdge(2, nodeB.getId(), targetNode.getId()),
                        createEdge(3, nodeA.getId(), nodeC.getId()),
                        createEdge(4, nodeC.getId(), targetNode.getId())
                ));

        var result = service.getProcessDataKeyHintResponses(targetNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("a", "a", null, nodeA),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("b", "b", null, nodeB),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("c", "c", null, nodeC)
                ),
                result.forwardedProcessDataKeys()
        );
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldHandleCyclesWithoutIncludingTheTargetNode() throws Exception {
        var nodeA = createNode(1, "a");
        var nodeB = createNode(2, "b");
        var targetNode = createNode(3, "target");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(targetNode, nodeB, nodeA));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(
                        createEdge(1, nodeA.getId(), nodeB.getId()),
                        createEdge(2, nodeB.getId(), nodeA.getId()),
                        createEdge(3, nodeB.getId(), targetNode.getId())
                ));

        var result = service.getProcessDataKeyHintResponses(targetNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("a", "a", null, nodeA),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("b", "b", null, nodeB)
                ),
                result.forwardedProcessDataKeys()
        );
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldIncludeMappedOutputProcessDataKeys() throws Exception {
        var nodeA = createNode(1, "a");
        nodeA.getOutputMappings().put("result", "foo.bar");
        var targetNode = createNode(2, "target");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(targetNode, nodeA));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(
                        createEdge(1, nodeA.getId(), targetNode.getId())
                ));

        var result = service.getProcessDataKeyHintResponses(targetNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("a", "a", null, nodeA),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("foo.bar", "Result", "Mapped test result.", nodeA)
                ),
                result.forwardedProcessDataKeys()
        );
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldKeepDuplicateKeysInTraversalOrder() throws Exception {
        var nodeA = createNode(1, "a");
        nodeA.getOutputMappings().put("result", "shared");
        var nodeB = createNode(2, "b");
        nodeB.getOutputMappings().put("result", "shared");
        var targetNode = createNode(3, "target");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(targetNode, nodeB, nodeA));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(
                        createEdge(1, nodeA.getId(), nodeB.getId()),
                        createEdge(2, nodeB.getId(), targetNode.getId())
                ));

        var result = service.getProcessDataKeyHintResponses(targetNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("a", "a", null, nodeA),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("shared", "Result", "Mapped test result.", nodeA),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("b", "b", null, nodeB),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("shared", "Result", "Mapped test result.", nodeB)
                ),
                result.forwardedProcessDataKeys()
        );
    }

    @Test
    void validate_ShouldApplyProviderValidationErrorsToDerivedRuntimeElementData() throws Exception {
        var provider = new FieldValidationTestNodeDefinition();
        var node = createNode(1, "a");

        var result = service.validate(node, provider, false);

        assertEquals(true, result.isPresent());
        var problems = result.orElseThrow();

        assertEquals(
                List.of(
                        "Validated field: First error.",
                        "Validated field: Second error."
                ),
                problems.problems()
        );
        assertEquals(
                "First error. Second error.",
                problems
                        .derivedRuntimeElementData()
                        .getElementStates()
                        .get(FieldValidationTestNodeDefinition.FIELD_ID)
                        .getError()
        );
    }

    @Test
    void create_ShouldRejectWhenNodeTypeLimitIsReached() {
        goverConfig.setProcessNodeLimits(Map.of(ProcessNodeType.Action, 1));
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createNode(1, "existing")));

        var exception = assertThrows(ResponseException.class, () -> service.create(createNode(2, "new")));

        assertTrue(exception.getMessage().contains("maximal 1"));
    }

    @Test
    void create_ShouldAllowNegativeNodeTypeLimit() throws Exception {
        goverConfig.setProcessNodeLimits(Map.of(ProcessNodeType.Action, -1));
        when(processNodeRepository.save(any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(createNode(2, "new"));

        assertEquals("new", result.getDataKey());
    }

    @Test
    void create_ShouldAllowLimitedNodeTypeWhenProcessUnlimitedFlagIsSet() throws Exception {
        goverConfig.setModuleFlags(List.of(ModuleFlags.PROCESS_UNLIMITED));
        goverConfig.setProcessNodeLimits(Map.of(ProcessNodeType.Action, 0));
        when(processNodeRepository.save(any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(createNode(2, "new"));

        assertEquals("new", result.getDataKey());
    }

    @Test
    void validateNewProcessNodeBatch_ShouldRejectDisabledFormPluginNodes() {
        var formService = createService(
                new ProcessNodeDefinitionService(List.of(new FormTestNodeDefinition())),
                new GoverConfig()
        );

        var exception = assertThrows(ResponseException.class, () -> formService.validateNewProcessNodeBatch(List.of(createFormNode())));

        assertTrue(exception.getMessage().contains("Formularerweiterung"));
    }

    private ProcessEntity createProcess() {
        return new ProcessEntity()
                .setId(PROCESS_ID)
                .setDepartmentId(7)
                .setInternalTitle("Process")
                .setAccessKey(UUID.randomUUID())
                .setVersionCount(1)
                .setCreated(Instant.now())
                .setUpdated(Instant.now());
    }

    private ProcessVersionEntity createProcessVersion() {
        return new ProcessVersionEntity()
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Draft")
                .setCreated(Instant.now())
                .setUpdated(Instant.now());
    }

    private ProcessNodeEntity createNode(int id, String dataKey) {
        return new ProcessNodeEntity()
                .setId(id)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Node " + id)
                .setDataKey(dataKey)
                .setProcessNodeDefinitionKey("test.process.hint-node")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(new HashMap<>())
                .setSavedWithErrors(false);
    }

    private ProcessNodeEntity createFormNode() {
        return new ProcessNodeEntity()
                .setId(1)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Form node")
                .setDataKey("form")
                .setProcessNodeDefinitionKey("de.aivot.form.form-trigger")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(new HashMap<>())
                .setSavedWithErrors(false);
    }

    private ProcessEdgeEntity createEdge(int id, int fromNodeId, int toNodeId) {
        return new ProcessEdgeEntity()
                .setId(id)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setFromNodeId(fromNodeId)
                .setToNodeId(toNodeId)
                .setViaPort("default");
    }

    private static final class HintingTestNodeDefinition implements ProcessNodeDefinition<HintingTestNodeDefinition.TestNodeConfig> {
        @Nonnull
        @Override
        public String getParentPluginKey() {
            return "test.process";
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return "hint-node";
        }

        @Nonnull
        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Nonnull
        @Override
        public String getName() {
            return "Hint node";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Test node definition for process data key hints.";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Nonnull
        @Override
        public List<ProcessNodeOutput> getOutputs() {
            return List.of(
                    new ProcessNodeOutput(
                            "result",
                            "Result",
                            "Mapped test result."
                    )
            );
        }

        @Override
        public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                         @Nonnull TestNodeConfig configuration,
                                                         @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
            return ProcessNodeDefinitionMetadata
                    .reuse(previousMetadata)
                    .addForwardedProcessDataKey(
                            processNodeEntity.getDataKey(),
                            processNodeEntity.getDataKey(),
                            null,
                            processNodeEntity
                    );
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<TestNodeConfig> context) {
            return new ProcessNodeExecutionResultTaskCompleted();
        }

        @Nonnull
        @Override
        public Class<TestNodeConfig> getNodeConfigurationClass() {
            return TestNodeConfig.class;
        }

        public static class TestNodeConfig {
        }
    }

    private static final class FieldValidationTestNodeDefinition implements ProcessNodeDefinition<FieldValidationTestNodeDefinition.TestNodeConfig> {
        private static final String FIELD_ID = "validatedField";

        @Nonnull
        @Override
        public String getParentPluginKey() {
            return "test.process";
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return "field-validation-node";
        }

        @Nonnull
        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Nonnull
        @Override
        public String getName() {
            return "Field validation node";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Test node definition for field validation errors.";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Nonnull
        @Override
        public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) {
            var layout = new ConfigLayoutElement();
            layout.setId(getKey() + "-config");

            var field = new TextInputElement();
            field.setId(FIELD_ID);
            field.setLabel("Validated field");
            layout.addChild(field);

            return layout;
        }

        @Override
        public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                               @Nonnull TestNodeConfig configuration) {
            return Map.of(FIELD_ID, List.of("First error.", "Second error."));
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<TestNodeConfig> context) {
            return new ProcessNodeExecutionResultTaskCompleted();
        }

        @Nonnull
        @Override
        public Class<TestNodeConfig> getNodeConfigurationClass() {
            return TestNodeConfig.class;
        }

        public static class TestNodeConfig {
        }
    }

    private static final class FormTestNodeDefinition implements ProcessNodeDefinition<FormTestNodeDefinition.TestNodeConfig> {
        @Nonnull
        @Override
        public String getParentPluginKey() {
            return FormPlugin.PLUGIN_KEY;
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return "form-trigger";
        }

        @Nonnull
        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Nonnull
        @Override
        public String getName() {
            return "Form trigger";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Test form trigger.";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Trigger;
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<TestNodeConfig> context) {
            return new ProcessNodeExecutionResultTaskCompleted();
        }

        @Nonnull
        @Override
        public Class<TestNodeConfig> getNodeConfigurationClass() {
            return TestNodeConfig.class;
        }

        public static class TestNodeConfig {
        }
    }
}

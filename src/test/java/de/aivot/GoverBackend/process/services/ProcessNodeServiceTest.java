package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.process.entities.ProcessEdgeEntity;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.GoverBackend.process.models.ProcessNodeOutput;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import de.aivot.GoverBackend.process.repositories.ProcessVersionRepository;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private ProcessNodeService service;

    @BeforeEach
    void setUp() {
        processNodeRepository = mock(ProcessNodeRepository.class);
        processEdgeRepository = mock(ProcessEdgeRepository.class);
        processRepository = mock(ProcessRepository.class);
        processVersionRepository = mock(ProcessVersionRepository.class);
        elementDerivationService = mock(ElementDerivationService.class);
        var userService = mock(UserService.class);

        var definitionService = new ProcessNodeDefinitionService(List.of(new HintingTestNodeDefinition()));

        service = new ProcessNodeService(
                processNodeRepository,
                definitionService,
                elementDerivationService,
                userService,
                processRepository,
                processVersionRepository,
                processEdgeRepository
        );

        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(createProcess()));
        when(processVersionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, PROCESS_VERSION)))
                .thenReturn(Optional.of(createProcessVersion()));
        when(elementDerivationService.derive(any()))
                .thenReturn(new DerivedRuntimeElementData());
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
}

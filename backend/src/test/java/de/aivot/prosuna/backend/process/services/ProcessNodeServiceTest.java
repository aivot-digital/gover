package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationAvailabilityService;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DepartmentSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.enums.InputModeEvaluationContext;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.services.ElementDerivationLogger;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.core.enums.ModuleFlags;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.plugins.form.FormPlugin;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessNodeServiceTest {
    private static final Integer PROCESS_ID = 10;
    private static final Integer PROCESS_VERSION = 3;
    private static final UUID IDENTITY_PROVIDER_KEY = UUID.fromString("2ff2831e-4726-4f0f-bb13-2d65075108e7");

    private ProcessNodeRepository processNodeRepository;
    private ProcessEdgeRepository processEdgeRepository;
    private ProcessRepository processRepository;
    private ProcessVersionRepository processVersionRepository;
    private ProcessTestClaimRepository processTestClaimRepository;
    private ElementDerivationService elementDerivationService;
    private IdentityCommunicationAvailabilityService identityCommunicationAvailabilityService;
    private ProsunaConfig prosunaConfig;
    private PermissionService permissionService;

    private ProcessNodeService service;

    @BeforeEach
    void setUp() {
        processNodeRepository = mock(ProcessNodeRepository.class);
        processEdgeRepository = mock(ProcessEdgeRepository.class);
        processRepository = mock(ProcessRepository.class);
        processVersionRepository = mock(ProcessVersionRepository.class);
        processTestClaimRepository = mock(ProcessTestClaimRepository.class);
        elementDerivationService = mock(ElementDerivationService.class);
        identityCommunicationAvailabilityService = mock(IdentityCommunicationAvailabilityService.class);
        prosunaConfig = new ProsunaConfig();
        permissionService = mock(PermissionService.class);

        when(identityCommunicationAvailabilityService.validate(any()))
                .thenReturn(new IdentityCommunicationAvailabilityService.ValidationResult(true, List.of()));

        var definitionService = new ProcessNodeDefinitionService(List.of(
                new HintingTestNodeDefinition(),
                new HintingTestNodeDefinition("trigger-hint-node", ProcessNodeType.Trigger),
                new InitialConfigurationTestNodeDefinition(),
                new DepartmentSelectionTestNodeDefinition()
        ));

        service = createService(
                definitionService,
                prosunaConfig
        );

        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(createProcess()));
        when(processVersionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, PROCESS_VERSION)))
                .thenReturn(Optional.of(createProcessVersion()));
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of());
        when(elementDerivationService.derive(any()))
                .thenReturn(new DerivedRuntimeElementData());
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(new DerivedRuntimeElementData());
    }

    private ProcessNodeService createService(ProcessNodeDefinitionService definitionService,
                                             ProsunaConfig config) {
        return new ProcessNodeService(
                processNodeRepository,
                definitionService,
                elementDerivationService,
                mock(UserService.class),
                processRepository,
                processVersionRepository,
                processEdgeRepository,
                config,
                identityCommunicationAvailabilityService,
                permissionService,
                new ProcessNodePortResolver(jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator()),
                new ProcessNodePortResolutionCache(de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils.createMapper()),
                new ProcessVersionDraftGuard(processVersionRepository, processTestClaimRepository)
        );
    }

    @Test
    void deadlineRequiresConnectionAndRemovedPortEdgesRemainValidationProblems() throws Exception {
        var node = createNode(1, "a").setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(1, false)));
        var provider = new HintingTestNodeDefinition();
        assertTrue(service.validate(node, provider, true).isEmpty());
        node.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, true)));
        var edge = createEdge(10, 1, 2).setViaPort("processing_overdue");
        assertTrue(service.validate(node, provider, true).orElseThrow().problems().stream()
                .anyMatch(problem -> problem.contains("Fristüberschreitung")));
        when(processEdgeRepository.existsByFromNodeIdAndViaPort(1, "processing_overdue")).thenReturn(true);
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION)).thenReturn(List.of(edge));
        assertTrue(service.validate(node, provider, true).isEmpty());

        node.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, false)));
        assertTrue(service.validate(node, provider, true).orElseThrow().problems().stream()
                .anyMatch(problem -> problem.contains("nicht mehr verfügbaren Ausgang")));
        org.mockito.Mockito.verify(processEdgeRepository, org.mockito.Mockito.never()).delete(any(ProcessEdgeEntity.class));
        assertEquals("processing_overdue", edge.getViaPort());
        node.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(null, true)));
        assertTrue(service.validate(node, provider, true).orElseThrow().problems().stream()
                .anyMatch(problem -> problem.contains("nicht mehr verfügbaren Ausgang")));
    }

    @Test
    void resolvesTypedAuthoringConfigurationAndExposesItInNodeResponse() throws Exception {
        var provider = new HintingTestNodeDefinition() {
            @Override
            public List<ProcessNodePort> getPorts(@Nonnull TestNodeConfig configuration) {
                return configuration.branch == null ? List.of() : List.of(new ProcessNodePort(configuration.branch, configuration.branch, "Fortsetzung"));
            }
        };
        service = createService(new ProcessNodeDefinitionService(List.of(provider)), prosunaConfig);
        var derived = new DerivedRuntimeElementData();
        derived.getEffectiveValues().put("branch", "approved");
        when(elementDerivationService.derive(any(), org.mockito.ArgumentMatchers.eq(InputModeEvaluationContext.Authoring))).thenReturn(derived);
        var node = createNode(1, "a").setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(2, true)));
        var response = de.aivot.prosuna.backend.process.dtos.ProcessNodeResponseDTO.of(node, service.resolvePorts(node));
        assertEquals(List.of("approved", "processing_overdue"), response.ports().stream().map(ProcessNodePort::key).toList());
        assertTrue(response.portResolutionProblems().isEmpty());
        org.mockito.Mockito.verify(elementDerivationService).derive(any(), org.mockito.ArgumentMatchers.eq(InputModeEvaluationContext.Authoring));
    }

    @Test
    void updatePersistsStrictnessAndRetainsTheSoftDueDate() throws Exception {
        var existing = createNode(1, "a").setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, false)));
        var submitted = createNode(1, "a").setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, true)));
        when(processNodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var strict = service.performUpdate(1, submitted, existing);
        assertTrue(strict.getDeadlines().get("processing").strict());

        var soft = service.performUpdate(1, submitted.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, false))), existing);
        assertEquals(false, soft.getDeadlines().get("processing").strict());
        assertEquals(3, soft.getDeadlines().get("processing").days());
        assertEquals(false, soft.getSavedWithErrors());
    }

    @Test
    void candidateResolutionPreparesDefaultsAndImportsWithoutSaving() throws Exception {
        var provider = new HintingTestNodeDefinition() {
            @Override
            public AuthoredElementValues getInitialConfiguration() {
                return new AuthoredElementValues().putLiteral("defaultOnly", "default").putLiteral("overridden", "default");
            }

            @Override
            public AuthoredElementValues prefillConfigurationOnImport(@Nonnull AuthoredElementValues configuration) {
                return configuration.putLiteral("imported", "prepared");
            }
        };
        service = createService(new ProcessNodeDefinitionService(List.of(provider)), prosunaConfig);
        var user = new de.aivot.prosuna.backend.user.entities.UserEntity().setId("staff");
        when(processVersionRepository.existsById(ProcessVersionEntityId.of(PROCESS_ID, PROCESS_VERSION))).thenReturn(true);
        var authored = new AuthoredElementValues().putLiteral("overridden", null);
        var node = createNode(1, "a").setConfiguration(authored);
        assertTrue(service.resolveCandidatePorts(user, node, true, true).problems().isEmpty());
        var request = org.mockito.ArgumentCaptor.forClass(de.aivot.prosuna.backend.elements.models.ElementDerivationRequest.class);
        verify(elementDerivationService).derive(request.capture(), org.mockito.ArgumentMatchers.eq(InputModeEvaluationContext.Authoring));
        var prepared = request.getValue().authoredElementValues();
        assertEquals("default", prepared.getLiteral("defaultOnly"));
        assertEquals("prepared", prepared.getLiteral("imported"));
        assertTrue(prepared.containsKey("overridden"));
        assertNull(prepared.getLiteral("overridden"));
        assertEquals(1, authored.size());
        org.mockito.Mockito.verify(processNodeRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void candidateResolutionChecksProcessPermissionBeforeReadingConfiguration() throws Exception {
        var systemPermissions = mock(de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository.class);
        var actualPermissions = new PermissionService(null, null, systemPermissions, null, null, processRepository, null);
        permissionService = actualPermissions;
        service = createService(new ProcessNodeDefinitionService(List.of(new HintingTestNodeDefinition())), prosunaConfig);
        var user = new de.aivot.prosuna.backend.user.entities.UserEntity().setId("staff");
        var request = new de.aivot.prosuna.backend.process.dtos.ProcessNodePortRequestDTO(createNode(1, "a"), false, false);
        when(processVersionRepository.existsById(ProcessVersionEntityId.of(PROCESS_ID, PROCESS_VERSION))).thenReturn(true);
        var permission = de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider.PROCESS_DEFINITION_READ;

        assertThrows(ResponseException.class, () -> service.resolveCandidatePorts(user, request.node(), request.forImport(), request.useInitialConfiguration()));
        verifyNoInteractions(elementDerivationService);
        when(processRepository.hasPermission("staff", PROCESS_ID, permission)).thenReturn(true);
        assertTrue(service.resolveCandidatePorts(user, request.node(), request.forImport(), request.useInitialConfiguration()).problems().isEmpty());
        request.node().setProcessId(PROCESS_ID + 1);
        assertThrows(ResponseException.class, () -> service.resolveCandidatePorts(user, request.node(), request.forImport(), request.useInitialConfiguration()));
        request.node().setProcessId(PROCESS_ID);
        when(processRepository.hasPermission("staff", PROCESS_ID, permission)).thenReturn(false);
        when(systemPermissions.hasPermission("staff", permission)).thenReturn(true);
        assertTrue(service.resolveCandidatePorts(user, request.node(), request.forImport(), request.useInitialConfiguration()).problems().isEmpty());
        org.mockito.Mockito.verify(processNodeRepository, org.mockito.Mockito.never()).save(any());
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

        var result = service.getIncomingProcessNodeDefinitionMetadata(targetNode);

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

        var result = service.getIncomingProcessNodeDefinitionMetadata(targetNode);

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

        var result = service.getIncomingProcessNodeDefinitionMetadata(targetNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("a", "a", null, nodeA),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("foo.bar", "Result", "Mapped test result.", nodeA)
                ),
                result.forwardedProcessDataKeys()
        );
        assertTrue(result.inputVariables().stream().anyMatch(variable ->
                variable.source() == InputVariableSource.ElementData &&
                        variable.path().equals("result") &&
                        variable.nodeDataKey().equals("a")
        ));
        for (var field : List.of("deadlineKey", "label", "deadline", "processedAt")) {
            assertTrue(result.inputVariables().stream().anyMatch(variable ->
                    variable.source() == InputVariableSource.ElementData &&
                            variable.path().equals("_deadlineExceeded." + field) && variable.nodeDataKey().equals("a")));
        }
        assertTrue(result.inputVariables().stream().anyMatch(variable ->
                variable.source() == InputVariableSource.ProcessData &&
                        variable.path().equals("foo.bar")
        ));
        assertTrue(result.inputVariables().stream().anyMatch(variable ->
                variable.source() == InputVariableSource.ProtectedProcessData &&
                        variable.path().equals("caseNumber")
        ));
    }

    // Blank output mappings never reach the key filter, so only non-blank invalid keys exercise it here.
    @ParameterizedTest
    @ValueSource(strings = {"person-name", "people..name", "people[", "people[2147483648]", "[0].name"})
    void incomingMetadata_ShouldSkipInvalidOutputMappings(String invalidKey) throws Exception {
        var source = createNode(1, "source");
        source.getOutputMappings().put("result", invalidKey);
        var validSource = createNode(2, "other");
        validSource.getOutputMappings().put("result", "people[0].name");
        var target = createNode(3, "target");
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(source, validSource, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2), createEdge(2, 2, 3)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(target);

        assertEquals(List.of("source", "other", "people[0].name"), result.forwardedProcessDataKeys().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedProcessDataKey::processDataKey).toList());
        assertEquals(List.of("source", "other", "people[0].name"), result.inputVariables().stream()
                .filter(variable -> variable.source() == InputVariableSource.ProcessData)
                .map(variable -> variable.path()).toList());
        assertEquals(List.of("source-identity", "other-identity"), result.forwardedIdentities().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedIdentity::identityId).toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void incomingMetadata_ShouldFilterProviderHintsAndPreserveOtherMetadata(boolean trigger) throws Exception {
        var source = createNode(1, "source");
        var target = createNode(2, "target");
        var layout = new GroupLayoutElement();
        layout.setId("form");
        var hints = Arrays.asList(null, "", " ", "person-name", "people..name", "people[",
                "people[2147483648]", "[0].name", "person.name", "people[0].name", "people[*].name", "person.name")
                .stream().map(key -> new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey(key, "Name", "Hint", source))
                .toList();
        // Providers may supply immutable lists; finalization must not remove entries from them.
        var metadata = new ProcessNodeDefinitionMetadata(new ArrayList<>(), new ArrayList<>(), hints, new ArrayList<>())
                .addReusableUiDefinition("Form", null, layout, source)
                .addForwardedAttachmentSet("files", "Files", null, true, source)
                .addForwardedIdentity("citizen", "Citizen", null, List.of(IDENTITY_PROVIDER_KEY), source);
        useMetadataProvider(metadata, trigger ? ProcessNodeType.Trigger : ProcessNodeType.Action);
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(source, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(trigger ? List.of() : List.of(createEdge(1, 1, 2)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(trigger ? source : target);

        assertEquals(hints.subList(8, 12), result.forwardedProcessDataKeys());
        assertEquals(List.of("person.name", "people[0].name"), result.inputVariables().stream()
                .filter(variable -> variable.source() == InputVariableSource.ProcessData)
                .map(variable -> variable.path()).toList());
        assertEquals(metadata.reusableUiDefinitions(), result.reusableUiDefinitions());
        assertEquals(metadata.forwardedAttachmentSets(), result.forwardedAttachmentSets());
        assertEquals(metadata.forwardedIdentities(), result.forwardedIdentities());
        assertTrue(result.inputVariables().stream().anyMatch(variable -> variable.source() == InputVariableSource.ElementData));
        assertTrue(result.inputVariables().stream().anyMatch(variable -> variable.source() == InputVariableSource.ElementMetadata));
        assertTrue(result.inputVariables().stream().anyMatch(variable -> variable.source() == InputVariableSource.ProtectedProcessData));
        assertEquals(hints, metadata.forwardedProcessDataKeys());
        assertTrue(metadata.inputVariables().stream().noneMatch(variable -> variable.source() == InputVariableSource.ProcessData));
    }

    @Test
    void incomingMetadata_ShouldForwardCanonicalDataKeys() throws Exception {
        var source = createNode(1, "source");
        var target = createNode(2, "target");
        var metadata = ProcessNodeDefinitionMetadata.empty()
                .addForwardedProcessDataKey(" person . name", "Name", null, source)
                .addForwardedProcessDataKey("people [*]. name", "Names", null, source)
                .addForwardedProcessDataKey("people[ 0 ].name ", "First name", null, source);
        useMetadataProvider(metadata, ProcessNodeType.Action);
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(source, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(target);

        assertEquals(List.of("person.name", "people[*].name", "people[0].name"), result.forwardedProcessDataKeys().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedProcessDataKey::processDataKey).toList());
        assertEquals(List.of("person.name", "people[0].name"), result.inputVariables().stream()
                .filter(variable -> variable.source() == InputVariableSource.ProcessData)
                .map(variable -> variable.path()).toList());
    }

    @Test
    void incomingMetadata_ShouldPreserveValueSourcesOfCanonicalizedDataKeys() throws Exception {
        var source = createNode(1, "source");
        var target = createNode(2, "target");
        var metadata = ProcessNodeDefinitionMetadata.empty()
                .addForwardedProcessDataKey(new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey(
                        " languages ", "Sprachen", null, source, null, "languages",
                        ProcessNodeDefinitionMetadata.ForwardedProcessDataKey.CodeListValueMode.LabelOnly))
                .addForwardedProcessDataKey("status", "Status", null, source, List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey.ValueOption("1", "Ledig")));
        useMetadataProvider(metadata, ProcessNodeType.Action);
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(source, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(target);

        assertEquals(List.of(
                new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey(
                        "languages", "Sprachen", null, source, null, "languages",
                        ProcessNodeDefinitionMetadata.ForwardedProcessDataKey.CodeListValueMode.LabelOnly),
                new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("status", "Status", null, source, List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey.ValueOption("1", "Ledig")), null, null)
        ), result.forwardedProcessDataKeys());
    }

    @Test
    void incomingMetadata_ShouldAddHintsToACopyOfIncompleteImmutableProviderMetadata() throws Exception {
        var source = createNode(1, "source");
        source.getOutputMappings().put("result", "mapped.result");
        var target = createNode(2, "target");
        var hint = new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("person.name", "Name", null, source);
        // Plugin providers may return immutable lists or leave lists unset despite the non-null contract.
        var metadata = new ProcessNodeDefinitionMetadata(null, null, List.of(hint), null, List.of());
        useMetadataProvider(metadata, ProcessNodeType.Action);
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(source, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(target);

        assertEquals(List.of("person.name", "mapped.result"), result.forwardedProcessDataKeys().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedProcessDataKey::processDataKey).toList());
        assertTrue(result.inputVariables().stream().anyMatch(variable -> variable.source() == InputVariableSource.ElementData));
        assertEquals(List.of(), result.reusableUiDefinitions());
        assertEquals(List.of(), result.forwardedAttachmentSets());
        assertEquals(List.of(), result.forwardedIdentities());
        assertEquals(List.of(hint), metadata.forwardedProcessDataKeys());
        assertEquals(List.of(), metadata.inputVariables());
    }

    @Test
    void incomingMetadata_ShouldKeepValidFormPathsIncludingWildcards() throws Exception {
        var source = createNode(1, "source");
        var target = createNode(2, "target");
        var invalidField = new TextInputElement();
        invalidField.setId("invalid");
        invalidField.setDestinationKey("person-name");
        var validField = new TextInputElement();
        validField.setId("valid");
        validField.setDestinationKey("person.name");
        var rowField = new TextInputElement();
        rowField.setId("row");
        rowField.setDestinationKey("name");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setDestinationKey("people");
        rows.setChildren(List.of(rowField));
        var layout = new GroupLayoutElement();
        layout.setId("form");
        layout.setChildren(List.of(invalidField, validField, rows));
        var metadata = ProcessNodeDefinitionMetadata.empty().withLayout(layout, source);
        useMetadataProvider(metadata, ProcessNodeType.Action);
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(source, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(target);

        assertEquals(List.of("person.name", "people", "people[*].name"), result.forwardedProcessDataKeys().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedProcessDataKey::processDataKey).toList());
        assertEquals(List.of("person.name", "people"), result.inputVariables().stream()
                .filter(variable -> variable.source() == InputVariableSource.ProcessData)
                .map(variable -> variable.path()).toList());
        assertEquals(metadata.reusableUiDefinitions(), result.reusableUiDefinitions());
    }

    @Test
    void incomingMetadata_ShouldSkipUpstreamNodesWhoseMetadataCannotBeCalculated() throws Exception {
        useBrokenMetadataProvider();
        var broken = createNode(1, "broken").setProcessNodeDefinitionKey("test.process.broken-node");
        var unavailable = createNode(2, "unavailable").setProcessNodeDefinitionKey("test.process.missing");
        var source = createNode(3, "source");
        var target = createNode(4, "target");
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(broken, unavailable, source, target));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2), createEdge(2, 2, 3), createEdge(3, 3, 4)));

        var result = service.getIncomingProcessNodeDefinitionMetadata(target);

        assertEquals(List.of("source"), result.forwardedProcessDataKeys().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedProcessDataKey::processDataKey).toList());
        assertEquals(List.of("source-identity"), result.forwardedIdentities().stream()
                .map(ProcessNodeDefinitionMetadata.ForwardedIdentity::identityId).toList());
        assertEquals(List.of("source"), result.inputVariables().stream()
                .filter(variable -> variable.source() == InputVariableSource.ProcessData)
                .map(variable -> variable.path()).toList());
    }

    private void useBrokenMetadataProvider() {
        var brokenProvider = new HintingTestNodeDefinition("broken-node", ProcessNodeType.Action) {
            @Override
            public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity node,
                                                             @Nonnull TestNodeConfig configuration,
                                                             @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
                throw new NullPointerException("Incomplete draft configuration.");
            }
        };
        service = createService(new ProcessNodeDefinitionService(List.of(new HintingTestNodeDefinition(), brokenProvider)), prosunaConfig);
    }

    private void useMetadataProvider(ProcessNodeDefinitionMetadata metadata, ProcessNodeType type) {
        var provider = new HintingTestNodeDefinition("hint-node", type) {
            @Override
            public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity node,
                                                             @Nonnull TestNodeConfig configuration,
                                                             @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
                return metadata;
            }
        };
        service = createService(new ProcessNodeDefinitionService(List.of(provider)), prosunaConfig);
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

        var result = service.getIncomingProcessNodeDefinitionMetadata(targetNode);

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
    void getProcessDataKeyHintResponses_ShouldReturnOwnMetadataForTriggerWithoutPreviousNodes() throws Exception {
        var triggerNode = createTriggerNode(1, "trigger");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(triggerNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of());

        var result = service.getIncomingProcessNodeDefinitionMetadata(triggerNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("trigger", "trigger", null, triggerNode)
                ),
                result.forwardedProcessDataKeys()
        );
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldIncludeMappedOutputsForTriggerWithoutPreviousNodes() throws Exception {
        var triggerNode = createTriggerNode(1, "trigger");
        triggerNode.getOutputMappings().put("result", "payload.result");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(triggerNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of());

        var result = service.getIncomingProcessNodeDefinitionMetadata(triggerNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("trigger", "trigger", null, triggerNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("payload.result", "Result", "Mapped test result.", triggerNode)
                ),
                result.forwardedProcessDataKeys()
        );
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldReturnEmptyMetadataForActionWithoutPreviousNodes() throws Exception {
        var actionNode = createNode(1, "action");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(actionNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of());

        var result = service.getIncomingProcessNodeDefinitionMetadata(actionNode);

        assertEquals(List.of(), result.forwardedProcessDataKeys());
    }

    @Test
    void getProcessDataKeyHintResponses_ShouldNotReturnOwnMetadataForTriggerWithPreviousNodes() throws Exception {
        var previousNode = createNode(1, "previous");
        var triggerNode = createTriggerNode(2, "trigger");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(triggerNode, previousNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(
                        createEdge(1, previousNode.getId(), triggerNode.getId())
                ));

        var result = service.getIncomingProcessNodeDefinitionMetadata(triggerNode);

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("previous", "previous", null, previousNode)
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
    void validate_ShouldListPaymentConfigurationDetailsIndividually() throws Exception {
        var provider = new HintingTestNodeDefinition() {
            @Nonnull
            @Override
            public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) {
                var field = new PaymentConfigElement();
                field.setId("payment");
                field.setLabel("Zahlung");
                var layout = new ConfigLayoutElement();
                layout.setId("configuration");
                layout.addChild(field);
                return layout;
            }
        };
        var data = new DerivedRuntimeElementData();
        data.getElementStates().put("payment", new ComputedElementState().setError("Konfiguration fehlerhaft.")
                .setErrorDetails(Map.of("messages", List.of("Beschreibung fehlt.", "Zahlungsposition 2: Betrag fehlt."))));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class))).thenReturn(data);

        var problems = service.validate(createNode(1, "a"), provider, false).orElseThrow();

        assertEquals(List.of("Zahlung: Beschreibung fehlt.", "Zahlung: Zahlungsposition 2: Betrag fehlt."), problems.problems());
    }

    @Test
    void validate_ShouldKeepProviderErrorsInConfigurationSummaries() throws Exception {
        var provider = new HintingTestNodeDefinition() {
            @Nonnull
            @Override
            public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) {
                var field = new PaymentConfigElement();
                field.setId("payment");
                field.setLabel("Zahlung");
                var layout = new ConfigLayoutElement();
                layout.setId("configuration");
                layout.addChild(field);
                return layout;
            }
            @Override
            public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<TestNodeConfig> context) {
                return Map.of("payment", List.of("Provider error."));
            }
        };
        var data = new DerivedRuntimeElementData();
        data.getElementStates().put("payment", new ComputedElementState().setError("Konfiguration fehlerhaft.")
                .setErrorDetails(Map.of("description", "Beschreibung fehlt.", "messages", List.of("Beschreibung fehlt.", "Zahlungsposition 2: Betrag fehlt."))));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class))).thenReturn(data);

        var problems = service.validate(createNode(1, "a"), provider, false).orElseThrow();

        assertEquals(List.of("Zahlung: Beschreibung fehlt.", "Zahlung: Zahlungsposition 2: Betrag fehlt.", "Zahlung: Provider error."), problems.problems());
        var details = (Map<?, ?>) data.getElementStates().get("payment").getErrorDetails();
        assertEquals("Beschreibung fehlt.", details.get("description"));
        assertEquals(List.of("Beschreibung fehlt.", "Zahlungsposition 2: Betrag fehlt.", "Provider error."), details.get("messages"));
    }

    @Test
    void validate_ShouldNotTreatADeferredDynamicValueAsMissing() throws Exception {
        var derivedData = new DerivedRuntimeElementData();
        derivedData.getElementStates().put(
                FieldValidationTestNodeDefinition.FIELD_ID,
                new ComputedElementState().setInputValueDeferred(true)
        );
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class))).thenReturn(derivedData);

        var result = service.validate(createNode(1, "a"), new ContextValidationTestNodeDefinition(), false);

        assertTrue(result.isEmpty());
    }

    @Test
    void runtimeConfigurationRejectsInvalidDeadlineDurationsBeforeNodeExecution() {
        var node = createNode(1, "a").setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(0, true)));
        assertThrows(ResponseException.class, () -> service.deriveRuntimeConfiguration(
                node, new HintingTestNodeDefinition(), null, false, new ProcessExecutionData()));
    }

    @Test
    void deriveRuntimeConfiguration_ShouldApplyOptedInProviderValidation() throws Exception {
        var derivedData = new DerivedRuntimeElementData();
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class))).thenReturn(derivedData);

        var result = service.deriveRuntimeConfiguration(
                createNode(1, "a"),
                new ContextValidationTestNodeDefinition(),
                null,
                false,
                new ProcessExecutionData()
        );

        assertEquals(
                "First error. Second error.",
                result.derivedRuntimeElementData()
                        .getElementStates()
                        .get(FieldValidationTestNodeDefinition.FIELD_ID)
                        .getError()
        );
    }

    @Test
    void deriveRuntimeConfiguration_ShouldRespectProviderAuthoringOnlyChecks() throws Exception {
        var result = service.deriveRuntimeConfiguration(
                createNode(1, "a"),
                new FieldValidationTestNodeDefinition(),
                null,
                false,
                new ProcessExecutionData()
        );

        assertTrue(result.derivedRuntimeElementData().getElementStates().isEmpty());
    }

    @Test
    void create_ShouldRejectWhenNodeTypeLimitIsReached() {
        prosunaConfig.setProcessNodeLimits(Map.of(ProcessNodeType.Action, 1));
        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createNode(1, "existing")));

        var exception = assertThrows(ResponseException.class, () -> service.create(createNode(2, "new")));

        assertTrue(exception.getMessage().contains("maximal 1"));
    }

    @Test
    void createReportsOnlyDataKeyCollisionsAsRetryable() {
        var node = createNode(2, "existing");
        var constraint = new org.hibernate.exception.ConstraintViolationException("duplicate",
                new java.sql.SQLException("duplicate", "23505"), "process_nodes_process_id_process_version_data_key_key");
        var failure = new org.springframework.dao.DataIntegrityViolationException("duplicate", constraint);
        when(processNodeRepository.saveAndFlush(any(ProcessNodeEntity.class))).thenThrow(failure);
        var error = assertThrows(ResponseException.class, () -> service.create(node));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatus());
        assertEquals(Map.of("reason", "process_node_data_key_conflict"), error.getDetails());
        var unrelated = new org.springframework.dao.DataIntegrityViolationException("foreign key",
                new org.hibernate.exception.ConstraintViolationException("foreign key",
                        new java.sql.SQLException("foreign key", "23503"), "process_nodes_process_id_fkey"));
        when(processNodeRepository.saveAndFlush(any(ProcessNodeEntity.class))).thenThrow(unrelated);
        assertEquals(unrelated, assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.create(node)));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessVersionStatus.class, names = {"Published", "Revoked"})
    void writes_ShouldRejectNodesOfVersionsThatAreNoLongerDrafts(ProcessVersionStatus status) {
        when(processVersionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, PROCESS_VERSION)))
                .thenReturn(Optional.of(createProcessVersion().setStatus(status)));
        var existingNode = createNode(2, "existing");

        assertConflict(() -> service.create(createNode(3, "new")));
        assertConflict(() -> service.performUpdate(2, createNode(2, "changed"), existingNode));
        assertConflict(() -> service.performDelete(existingNode));

        assertEquals("existing", existingNode.getDataKey());
        verify(processNodeRepository, never()).saveAndFlush(any(ProcessNodeEntity.class));
        verify(processNodeRepository, never()).save(any(ProcessNodeEntity.class));
        verify(processNodeRepository, never()).delete(any(ProcessNodeEntity.class));
    }

    @Test
    void performUpdate_ShouldRejectMovingNodeIntoPublishedVersion() {
        var publishedVersion = PROCESS_VERSION - 1;
        when(processVersionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, publishedVersion)))
                .thenReturn(Optional.of(createProcessVersion()
                        .setProcessVersion(publishedVersion)
                        .setStatus(ProcessVersionStatus.Published)));
        var existingNode = createNode(2, "existing");

        assertConflict(() -> service.performUpdate(2, createNode(2, "existing").setProcessVersion(publishedVersion), existingNode));

        assertEquals(PROCESS_VERSION, existingNode.getProcessVersion());
        verify(processNodeRepository, never()).save(any(ProcessNodeEntity.class));
    }

    @Test
    void performDelete_ShouldDeleteNodesOfDrafts() throws Exception {
        var node = createNode(2, "existing");

        service.performDelete(node);

        verify(processNodeRepository).delete(node);
    }

    @Test
    void writes_ShouldRejectAddingAndRemovingNodesOfDraftWithTestClaim() {
        when(processTestClaimRepository.existsByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION)).thenReturn(true);
        var existingNode = createNode(2, "existing");

        assertConflict(() -> service.create(createNode(3, "new")));
        assertConflict(() -> service.performDelete(existingNode));

        verify(processNodeRepository, never()).saveAndFlush(any(ProcessNodeEntity.class));
        verify(processNodeRepository, never()).delete(any(ProcessNodeEntity.class));
    }

    @Test
    void performUpdate_ShouldEditNodesOfDraftWithTestClaim() throws Exception {
        when(processTestClaimRepository.existsByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION)).thenReturn(true);
        when(processNodeRepository.save(any(ProcessNodeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var existingNode = createNode(2, "existing");
        var changedNode = createNode(2, "changed").setName("Changed node");
        changedNode.getConfiguration().putLiteral("branch", "approved");

        var result = service.performUpdate(2, changedNode, existingNode);

        assertEquals("changed", result.getDataKey());
        assertEquals("Changed node", result.getName());
        assertEquals("approved", result.getConfiguration().getLiteral("branch"));
        verify(processNodeRepository).save(existingNode);
    }

    @Test
    void performUpdate_ShouldRejectReplacingNodeTypeOfDraftWithTestClaim() {
        when(processTestClaimRepository.existsByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION)).thenReturn(true);
        var existingNode = createNode(2, "existing");

        assertConflict(() -> service.performUpdate(2, createNode(2, "existing")
                .setProcessNodeDefinitionKey("test.process.initial-configuration-node"), existingNode));
        assertConflict(() -> service.performUpdate(2, createNode(2, "existing")
                .setProcessNodeDefinitionVersion(2), existingNode));

        assertEquals("test.process.hint-node", existingNode.getProcessNodeDefinitionKey());
        assertEquals(1, existingNode.getProcessNodeDefinitionVersion());
        verify(processNodeRepository, never()).save(any(ProcessNodeEntity.class));
    }

    @Test
    void performUpdate_ShouldReplaceNodeTypeOfDraftWithoutTestClaim() throws Exception {
        when(processNodeRepository.save(any(ProcessNodeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var existingNode = createNode(2, "existing");

        var result = service.performUpdate(2, createNode(2, "existing")
                .setProcessNodeDefinitionKey("test.process.initial-configuration-node"), existingNode);

        assertEquals("test.process.initial-configuration-node", result.getProcessNodeDefinitionKey());
        verify(processNodeRepository).save(existingNode);
    }

    @Test
    void performUpdate_ShouldRejectMovingNodesIntoOrOutOfDraftWithTestClaim() {
        var testedVersion = PROCESS_VERSION + 1;
        when(processVersionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, testedVersion)))
                .thenReturn(Optional.of(createProcessVersion().setProcessVersion(testedVersion)));
        when(processTestClaimRepository.existsByProcessIdAndProcessVersion(PROCESS_ID, testedVersion)).thenReturn(true);
        var draftNode = createNode(2, "existing");
        var testedNode = createNode(3, "tested").setProcessVersion(testedVersion);

        assertConflict(() -> service.performUpdate(2, createNode(2, "existing").setProcessVersion(testedVersion), draftNode));
        assertConflict(() -> service.performUpdate(3, createNode(3, "tested"), testedNode));

        assertEquals(PROCESS_VERSION, draftNode.getProcessVersion());
        assertEquals(testedVersion, testedNode.getProcessVersion());
        verify(processNodeRepository, never()).save(any(ProcessNodeEntity.class));
    }

    @Test
    void create_ShouldAllowNegativeNodeTypeLimit() throws Exception {
        prosunaConfig.setProcessNodeLimits(Map.of(ProcessNodeType.Action, -1));
        when(processNodeRepository.saveAndFlush(any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(createNode(2, "new"));

        assertEquals("new", result.getDataKey());
    }

    @Test
    void create_ShouldAllowLimitedNodeTypeWhenProcessFlagIsSet() throws Exception {
        prosunaConfig.setModuleFlags(List.of(ModuleFlags.PROCESS));
        prosunaConfig.setProcessNodeLimits(Map.of(ProcessNodeType.Action, 0));
        when(processNodeRepository.saveAndFlush(any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(createNode(2, "new"));

        assertEquals("new", result.getDataKey());
    }

    @Test
    void create_ShouldFillMissingInitialConfigurationWithoutOverwritingSubmittedValues() throws Exception {
        var node = createNode(2, "new")
                .setProcessNodeDefinitionKey("test.process.initial-configuration-node");
        node.getConfiguration()
                .putLiteral("overridden", null);
        when(processNodeRepository.saveAndFlush(any(ProcessNodeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(node);

        assertEquals("default", result.getConfiguration().getLiteral("defaultOnly"));
        assertTrue(result.getConfiguration().containsKey("overridden"));
        assertNull(result.getConfiguration().getLiteral("overridden"));
    }

    @Test
    void validateNewProcessNodeBatch_ShouldRejectDisabledFormPluginNodes() {
        var formService = createService(
                new ProcessNodeDefinitionService(List.of(new FormTestNodeDefinition())),
                new ProsunaConfig()
        );

        var exception = assertThrows(ResponseException.class, () -> formService.validateNewProcessNodeBatch(List.of(createFormNode())));

        assertTrue(exception.getMessage().contains("Formularerweiterung"));
    }

    @Test
    void requireReadableDepartmentSelectionsChecksTheSelectedDepartmentScope() throws Exception {
        var node = createNode(1, "department")
                .setProcessNodeDefinitionKey("test.process.department-selection-node");
        node.getConfiguration().putLiteral(DepartmentSelectionTestNodeDefinition.FIELD_ID, 42);
        var user = new UserEntity().setId("user-1");

        service.requireReadableDepartmentSelections(user, node);

        verify(permissionService).requireDepartmentPermission(
                "user-1",
                42,
                DepartmentPermissionProvider.DEPARTMENT_READ
        );
    }

    @Test
    void requireReadableDepartmentSelectionsPropagatesDeniedDepartmentScope() throws Exception {
        var node = createNode(1, "department")
                .setProcessNodeDefinitionKey("test.process.department-selection-node");
        node.getConfiguration().putLiteral(DepartmentSelectionTestNodeDefinition.FIELD_ID, 42);
        var user = new UserEntity().setId("user-1");
        doThrow(ResponseException.forbidden("forbidden"))
                .when(permissionService)
                .requireDepartmentPermission(
                        "user-1",
                        42,
                        DepartmentPermissionProvider.DEPARTMENT_READ
                );

        var exception = assertThrows(
                ResponseException.class,
                () -> service.requireReadableDepartmentSelections(user, node)
        );

        assertEquals("forbidden", exception.getMessage());
    }

    @Test
    void deriveUiDefinitionForAuthoring_ShouldDeriveStoredUiDefinitionWithoutDraft() throws Exception {
        var storedUiDefinition = new GroupLayoutElement();
        storedUiDefinition.setId("stored");
        var node = createNode(1, "a");
        node.getConfiguration().putLiteral(UiDefinitionTestNodeDefinition.FIELD_ID, storedUiDefinition);
        var identities = new IdentityDataMap();

        service.deriveUiDefinitionForAuthoring(
                node,
                new UiDefinitionTestNodeDefinition(),
                null,
                UiDefinitionTestNodeDefinition.FIELD_ID,
                null,
                new AuthoredElementValues(),
                new ElementDerivationOptions(),
                identities
        );

        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        verify(elementDerivationService).derive(request.capture(), eq(identities), any(ElementDerivationLogger.class));
        assertEquals("stored", request.getValue().element().getId());
    }

    @Test
    void deriveUiDefinitionForAuthoring_ShouldDeriveDraftInsteadOfStoredUiDefinition() throws Exception {
        var storedUiDefinition = new GroupLayoutElement();
        storedUiDefinition.setId("stored");
        var node = createNode(1, "a");
        node.getConfiguration().putLiteral(UiDefinitionTestNodeDefinition.FIELD_ID, storedUiDefinition);
        var draftUiDefinition = new GroupLayoutElement();
        draftUiDefinition.setId("draft");
        var identities = new IdentityDataMap();

        service.deriveUiDefinitionForAuthoring(
                node,
                new UiDefinitionTestNodeDefinition(),
                null,
                UiDefinitionTestNodeDefinition.FIELD_ID,
                draftUiDefinition,
                new AuthoredElementValues(),
                new ElementDerivationOptions(),
                identities
        );

        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        verify(elementDerivationService).derive(request.capture(), eq(identities), any(ElementDerivationLogger.class));
        assertSame(draftUiDefinition, request.getValue().element());
    }

    @Test
    void deriveUiDefinitionForAuthoring_ShouldRejectFieldsThatAreNoUiDefinitionFields() {
        var exception = assertThrows(ResponseException.class, () -> service.deriveUiDefinitionForAuthoring(
                createNode(1, "a"),
                new UiDefinitionTestNodeDefinition(),
                null,
                UiDefinitionTestNodeDefinition.OTHER_FIELD_ID,
                new GroupLayoutElement(),
                new AuthoredElementValues(),
                new ElementDerivationOptions(),
                new IdentityDataMap()
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(elementDerivationService, never()).derive(any(), any(IdentityDataMap.class), any(ElementDerivationLogger.class));
    }

    @Test
    void deriveUiDefinitionForAuthoring_ShouldRejectDraftsOfAnotherElementType() {
        var exception = assertThrows(ResponseException.class, () -> service.deriveUiDefinitionForAuthoring(
                createNode(1, "a"),
                new UiDefinitionTestNodeDefinition(),
                null,
                UiDefinitionTestNodeDefinition.FIELD_ID,
                new ConfigLayoutElement(),
                new AuthoredElementValues(),
                new ElementDerivationOptions(),
                new IdentityDataMap()
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(elementDerivationService, never()).derive(any(), any(IdentityDataMap.class), any(ElementDerivationLogger.class));
    }

    @Test
    void deriveUiDefinitionForAuthoring_ShouldReturnEmptyDataWithoutStoredUiDefinition() throws Exception {
        var result = service.deriveUiDefinitionForAuthoring(
                createNode(1, "a"),
                new UiDefinitionTestNodeDefinition(),
                null,
                UiDefinitionTestNodeDefinition.FIELD_ID,
                null,
                new AuthoredElementValues(),
                new ElementDerivationOptions(),
                new IdentityDataMap()
        );

        assertTrue(result.getEffectiveValues().isEmpty());
        verify(elementDerivationService, never()).derive(any(), any(IdentityDataMap.class), any(ElementDerivationLogger.class));
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

    private static void assertConflict(Executable executable) {
        var exception = assertThrows(ResponseException.class, executable);
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
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

    private ProcessNodeEntity createTriggerNode(int id, String dataKey) {
        return createNode(id, dataKey)
                .setProcessNodeDefinitionKey("test.process.trigger-hint-node");
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

    private static class HintingTestNodeDefinition implements ProcessNodeDefinition<HintingTestNodeDefinition.TestNodeConfig> {
        private final String componentKey;
        private final ProcessNodeType type;

        private HintingTestNodeDefinition() {
            this("hint-node", ProcessNodeType.Action);
        }

        private HintingTestNodeDefinition(String componentKey,
                                          ProcessNodeType type) {
            this.componentKey = componentKey;
            this.type = type;
        }

        @Nonnull
        @Override
        public String getParentPluginKey() {
            return "test.process";
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return componentKey;
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
        public String getAbstract() {
            return "Test node definition for process data key hints.";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Test node definition for process data key hints.";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return type;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
        }

        @Override
        public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull TestNodeConfig configuration) {
            return List.of(new ProcessNodeDeadlineDefinition("processing", "Bearbeitung", "Bearbeitungsfrist", new ProcessNodePort("processing_overdue", "Fristüberschreitung", "Fortsetzung")));
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts(@Nonnull TestNodeConfig configuration) {
            return List.of();
        }

        @Nonnull
        @Override
        public List<ProcessNodeOutput> getOutputs() {
            return List.of(
                    new ProcessNodeOutput(
                            "result",
                            "Result",
                            "Mapped test result.",
                            "string"
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
                    )
                    .addForwardedIdentity(
                            processNodeEntity.getDataKey() + "-identity",
                            processNodeEntity.getDataKey() + " identity",
                            null,
                            List.of(IDENTITY_PROVIDER_KEY),
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
            @InputElementPOJOBinding(id = "branch", type = ElementType.Text)
            public String branch;
        }
    }

    @Test
    void validate_ShouldAcceptForwardedProcessIdentityId() throws Exception {
        var provider = new IdentityIdValidationTestNodeDefinition();
        var sourceNode = createNode(1, "source");
        var targetNode = createNode(2, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "source-identity");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(sourceNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, sourceNode.getId(), targetNode.getId())));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());

        var result = service.validate(targetNode, provider, false);

        assertTrue(result.isEmpty());
        verifyNoInteractions(identityCommunicationAvailabilityService);
    }

    @Test
    void validate_ShouldRejectForwardedIdentityWithoutCommunicationBindingForCommunicatingField() throws Exception {
        var provider = new IdentityIdValidationTestNodeDefinition(false, true);
        var sourceNode = createNode(1, "source");
        var targetNode = createNode(2, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "source-identity");
        var expectedError = "Für den Identitätsanbieter \"BayernID\" (source identity) ist keine verwendbare Kommunikationsanbindung konfiguriert.";

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(sourceNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, sourceNode.getId(), targetNode.getId())));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());
        when(identityCommunicationAvailabilityService.validate(any()))
                .thenReturn(new IdentityCommunicationAvailabilityService.ValidationResult(
                        true,
                        List.of(expectedError)
                ));

        var problems = service.validate(targetNode, provider, false).orElseThrow();

        assertEquals(List.of("Identity: " + expectedError), problems.problems());
        assertEquals(
                expectedError,
                problems
                        .derivedRuntimeElementData()
                        .getElementStates()
                        .get(IdentityIdValidationTestNodeDefinition.FIELD_ID)
                        .getError()
        );
        verify(identityCommunicationAvailabilityService).validate(List.of(
                new IdentityCommunicationAvailabilityService.IdentityProviderUsage(
                        IDENTITY_PROVIDER_KEY,
                        "source identity"
                )
        ));
    }

    @Test
    void validate_ShouldRejectUnavailableProcessIdentityId() throws Exception {
        var provider = new IdentityIdValidationTestNodeDefinition();
        var sourceNode = createNode(1, "source");
        var targetNode = createNode(2, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "missing-identity");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(sourceNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, sourceNode.getId(), targetNode.getId())));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());

        var problems = service.validate(targetNode, provider, false).orElseThrow();
        var expectedError = "Die ausgewählte Prozessidentität „missing-identity“ ist nicht mehr verfügbar.";

        assertEquals(List.of("Identity: " + expectedError), problems.problems());
        assertEquals(
                expectedError,
                problems
                        .derivedRuntimeElementData()
                        .getElementStates()
                        .get(IdentityIdValidationTestNodeDefinition.FIELD_ID)
                        .getError()
        );
    }

    @Test
    void validate_ShouldRejectProcessIdentityIdWhenIncomingMetadataCannotBeResolved() throws Exception {
        var provider = new IdentityIdValidationTestNodeDefinition();
        var sourceNode = createNode(1, "source")
                .setProcessNodeDefinitionKey("test.process.missing");
        var targetNode = createNode(2, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "source-identity");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(sourceNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, sourceNode.getId(), targetNode.getId())));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());

        var problems = service.validate(targetNode, provider, false).orElseThrow();
        var expectedError = "Die verfügbaren Prozessidentitäten konnten nicht ermittelt werden.";

        assertEquals(List.of("Identity: " + expectedError), problems.problems());
        assertEquals(
                expectedError,
                problems
                        .derivedRuntimeElementData()
                        .getElementStates()
                        .get(IdentityIdValidationTestNodeDefinition.FIELD_ID)
                        .getError()
        );
    }

    @Test
    void validate_ShouldReportUnresolvableProcessIdentitiesWhenUpstreamMetadataFails() throws Exception {
        useBrokenMetadataProvider();
        var provider = new IdentityIdValidationTestNodeDefinition();
        var brokenNode = createNode(1, "broken").setProcessNodeDefinitionKey("test.process.broken-node");
        var targetNode = createNode(2, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "broken-identity");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(brokenNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, brokenNode.getId(), targetNode.getId())));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());

        var problems = service.validate(targetNode, provider, false).orElseThrow();

        assertEquals(
                List.of("Identity: Die verfügbaren Prozessidentitäten konnten nicht ermittelt werden."),
                problems.problems()
        );
    }

    @Test
    void validate_ShouldAcceptAvailableProcessIdentityWhenAnotherUpstreamNodeIsSkipped() throws Exception {
        useBrokenMetadataProvider();
        var provider = new IdentityIdValidationTestNodeDefinition();
        var brokenNode = createNode(1, "broken").setProcessNodeDefinitionKey("test.process.broken-node");
        var sourceNode = createNode(2, "source");
        var targetNode = createNode(3, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "source-identity");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(brokenNode, sourceNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, 1, 2), createEdge(2, 2, 3)));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());

        var result = service.validate(targetNode, provider, false);

        assertTrue(result.isEmpty());
    }

    @Test
    void validate_ShouldMergeUnavailableProcessIdentityAndProviderErrors() throws Exception {
        var provider = new IdentityIdValidationTestNodeDefinition(true);
        var sourceNode = createNode(1, "source");
        var targetNode = createNode(2, "target");
        var derivedRuntimeElementData = DerivedRuntimeElementData.empty();
        derivedRuntimeElementData
                .getEffectiveValues()
                .put(IdentityIdValidationTestNodeDefinition.FIELD_ID, "missing-identity");

        when(processNodeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(sourceNode, targetNode));
        when(processEdgeRepository.findAllByProcessIdAndProcessVersion(PROCESS_ID, PROCESS_VERSION))
                .thenReturn(List.of(createEdge(1, sourceNode.getId(), targetNode.getId())));
        when(elementDerivationService.derive(any(), any(InputModeEvaluationContext.class)))
                .thenReturn(derivedRuntimeElementData, DerivedRuntimeElementData.empty());

        var problems = service.validate(targetNode, provider, false).orElseThrow();

        assertEquals(
                "Die ausgewählte Prozessidentität „missing-identity“ ist nicht mehr verfügbar. Provider error.",
                problems
                        .derivedRuntimeElementData()
                        .getElementStates()
                        .get(IdentityIdValidationTestNodeDefinition.FIELD_ID)
                        .getError()
        );
    }

    private static final class IdentityIdValidationTestNodeDefinition implements ProcessNodeDefinition<IdentityIdValidationTestNodeDefinition.TestNodeConfig> {
        private static final String FIELD_ID = "identity";

        private final boolean includeProviderError;
        private final boolean requiresCommunication;

        private IdentityIdValidationTestNodeDefinition() {
            this(false, false);
        }

        private IdentityIdValidationTestNodeDefinition(boolean includeProviderError) {
            this(includeProviderError, false);
        }

        private IdentityIdValidationTestNodeDefinition(boolean includeProviderError,
                                                       boolean requiresCommunication) {
            this.includeProviderError = includeProviderError;
            this.requiresCommunication = requiresCommunication;
        }

        @Nonnull
        @Override
        public String getParentPluginKey() {
            return "test.process";
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return "identity-id-validation-node";
        }

        @Nonnull
        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Nonnull
        @Override
        public String getName() {
            return "Identity ID validation node";
        }

        @Nonnull
        @Override
        public String getAbstract() {
            return "Test node definition for process identity ID validation.";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Test node definition for process identity ID validation.";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts(@Nonnull TestNodeConfig configuration) {
            return List.of();
        }

        @Nonnull
        @Override
        public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) {
            var layout = new ConfigLayoutElement();
            layout.setId(getKey() + "-config");

            var field = new ProcessIdentityIdInputElement();
            field.setId(FIELD_ID);
            field.setLabel("Identity");
            field.setRequiresCommunication(requiresCommunication);
            layout.addChild(field);

            return layout;
        }

        @Override
        public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<TestNodeConfig> context) {
            return includeProviderError
                    ? Map.of(FIELD_ID, List.of("Provider error."))
                    : Map.of();
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
            @InputElementPOJOBinding(id = FIELD_ID, type = ElementType.ProcessIdentityIdInput)
            public String identityId;
        }
    }

    private static final class InitialConfigurationTestNodeDefinition extends HintingTestNodeDefinition {
        private InitialConfigurationTestNodeDefinition() {
            super("initial-configuration-node", ProcessNodeType.Action);
        }

        @Nonnull
        @Override
        public AuthoredElementValues getInitialConfiguration() {
            return new AuthoredElementValues()
                    .putLiteral("defaultOnly", "default")
                    .putLiteral("overridden", "default");
        }
    }

    private static final class UiDefinitionTestNodeDefinition extends HintingTestNodeDefinition {
        private static final String FIELD_ID = "uiDefinition";
        private static final String OTHER_FIELD_ID = "other";

        private UiDefinitionTestNodeDefinition() {
            super("ui-definition-node", ProcessNodeType.Action);
        }

        @Nonnull
        @Override
        public ConfigLayoutElement getConfigurationLayout(
                @Nonnull ProcessNodeDefinitionConfigurationLayoutContext context
        ) {
            var layout = new ConfigLayoutElement();
            layout.setId(getKey() + "-config");

            var uiDefinitionField = new UiDefinitionInputElement()
                    .setElementType(ElementType.GroupLayout);
            uiDefinitionField.setId(FIELD_ID);
            uiDefinitionField.setLabel("UI definition");
            layout.addChild(uiDefinitionField);

            var otherField = new TextInputElement();
            otherField.setId(OTHER_FIELD_ID);
            otherField.setLabel("Other");
            layout.addChild(otherField);

            return layout;
        }
    }

    private static final class DepartmentSelectionTestNodeDefinition extends HintingTestNodeDefinition {
        private static final String FIELD_ID = "department";

        private DepartmentSelectionTestNodeDefinition() {
            super("department-selection-node", ProcessNodeType.Action);
        }

        @Nonnull
        @Override
        public ConfigLayoutElement getConfigurationLayout(
                @Nonnull ProcessNodeDefinitionConfigurationLayoutContext context
        ) {
            var layout = new ConfigLayoutElement();
            layout.setId(getKey() + "-config");
            var field = new DepartmentSelectInputElement();
            field.setId(FIELD_ID);
            field.setLabel("Department");
            layout.addChild(field);
            return layout;
        }
    }

    private static class FieldValidationTestNodeDefinition implements ProcessNodeDefinition<FieldValidationTestNodeDefinition.TestNodeConfig> {
        static final String FIELD_ID = "validatedField";

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
        public String getAbstract() {
            return "Test node definition for field validation errors.";
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
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts(@Nonnull TestNodeConfig configuration) {
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
        public Map<String, List<String>> validateConfiguration(
                @Nonnull ProcessNodeConfigurationValidationContext<TestNodeConfig> context
        ) {
            if (!context.isAuthoring()) {
                return null;
            }
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

    private static final class ContextValidationTestNodeDefinition extends FieldValidationTestNodeDefinition {
        @Override
        public Map<String, List<String>> validateConfiguration(
                @Nonnull ProcessNodeConfigurationValidationContext<TestNodeConfig> context
        ) {
            if (context.isDeferred(FIELD_ID)) {
                return null;
            }

            return Map.of(FIELD_ID, List.of("First error.", "Second error."));
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
        public String getAbstract() {
            return "Test form trigger.";
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
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts(@Nonnull TestNodeConfig configuration) {
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

package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.audit.services.*;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.*;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatProcessServiceTest {
    private final AiProcessChatContext scope = new AiProcessChatContext("owner", "session", 7, 2);
    private final VUserSystemPermissionRepository systemPermissions = mock(VUserSystemPermissionRepository.class);
    private final ProcessRepository processes = mock(ProcessRepository.class);
    private final PermissionService permissions = new PermissionService(mock(VUserDepartmentPermissionRepository.class), mock(VUserTeamPermissionRepository.class),
            systemPermissions, mock(DepartmentRepository.class), mock(TeamRepository.class), processes, mock(ProcessInstanceRepository.class));
    private final AiChatSessionRepository sessions = mock(AiChatSessionRepository.class);
    private final UserService users = mock(UserService.class);
    private final ProcessVersionRepository versions = mock(ProcessVersionRepository.class);
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final ProcessNodeService nodeService = mock(ProcessNodeService.class);
    private final ProcessEdgeService edgeService = new ProcessEdgeService(edges);
    private final ProcessVersionService versionService = mock(ProcessVersionService.class);
    private final ProcessNodeDefinitionService definitions = mock(ProcessNodeDefinitionService.class);
    private final ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
    private final AiProcessConfigurationService configuration = mock(AiProcessConfigurationService.class);
    private final EntityManager em = mock(EntityManager.class);
    private final AuditService audits = mock(AuditService.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final UserEntity actor = new UserEntity().setId("owner");
    private final ProcessVersionEntity version = new ProcessVersionEntity().setProcessId(7).setProcessVersion(2).setStatus(ProcessVersionStatus.Drafted);
    private final ProcessNodeEntity node = node(10, 7, 2);
    private AiChatProcessService service;

    @BeforeEach
    void setUp() throws Exception {
        var mapper = JsonMapperTestUtils.createMapper();
        when(systemPermissions.hasPermission("owner", AiChatPermissionProvider.AI_CHAT_USE)).thenReturn(true);
        when(processes.hasPermission("owner", 7, ProcessPermissionProvider.PROCESS_DEFINITION_READ)).thenReturn(true);
        when(processes.hasPermission("owner", 7, ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        when(sessions.findByUserIdAndSessionId("owner", "session")).thenReturn(Optional.of(new AiChatSessionEntity()));
        when(users.retrieve("owner")).thenReturn(Optional.of(actor));
        when(versions.findById(ProcessVersionEntityId.of(7, 2))).thenReturn(Optional.of(version));
        when(nodes.findById(10)).thenReturn(Optional.of(node));
        when(nodes.findAllByProcessIdAndProcessVersion(7, 2)).thenReturn(List.of(node));
        when(nodes.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(definitions.getProcessNodeDefinition(any(ProcessNodeEntity.class))).thenReturn(Optional.of(definition));
        when(nodeService.validate(any(), any(), eq(false), eq(actor))).thenReturn(Optional.empty());
        when(nodeService.updateForAuthoring(anyInt(), any(), any(), eq(actor))).thenAnswer(i -> i.getArgument(1));
        when(configuration.patch(any(), eq(actor), any(), any())).thenAnswer(i -> ((ProcessNodeEntity) i.getArgument(0)).getConfiguration());
        when(audits.createScopedAuditService(any(), anyString())).thenReturn(new ScopedAuditService(getClass(), "Prozesse", auditLogs));
        service = new AiChatProcessService(permissions, sessions, users, versions, nodes, edges, nodeService, edgeService, versionService,
                definitions, configuration, mock(AiProcessOptionsService.class), new AiProcessFormService(mapper), mapper, em,
                Validation.buildDefaultValidatorFactory().getValidator(), audits);
    }

    @Test
    void checksOwnedSessionAndAiPermissionBeforeLoadingProcess() throws Exception {
        when(sessions.findByUserIdAndSessionId("owner", "session")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireContext(scope)).isInstanceOf(ResponseException.class);
        verifyNoInteractions(versions);
        when(systemPermissions.hasPermission("owner", AiChatPermissionProvider.AI_CHAT_USE)).thenReturn(false);
        assertThatThrownBy(() -> service.requireContext(scope)).isInstanceOf(ResponseException.class);
        verifyNoInteractions(nodeService, auditLogs);
    }

    @Test
    void enforcesCorrectProcessGrantAndSystemOverride() throws Exception {
        when(processes.hasPermission("owner", 7, ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(false);
        when(processes.hasPermission("owner", 8, ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        assertThatThrownBy(() -> service.updateNode(scope, 10, Map.of("name", "New"), Map.of(), List.of())).isInstanceOf(ResponseException.class);
        verifyNoInteractions(nodeService, auditLogs);
        when(systemPermissions.hasPermission("owner", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        service.updateNode(scope, 10, Map.of("name", "New"), Map.of(), List.of());
        verify(nodeService).updateForAuthoring(eq(10), argThat(n -> n.getName().equals("New")), eq(node), eq(actor));
        verify(em).refresh(version, LockModeType.PESSIMISTIC_WRITE);
        verify(em).refresh(node, LockModeType.PESSIMISTIC_WRITE);
        verify(auditLogs).create(argThat(log -> log.getActorId().equals("owner") && log.getTriggerType().equals("Update") && log.getDiff() != null));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessVersionStatus.class, names = {"Published", "Revoked"})
    void rejectsNonDraftWritesButAllowsReading(ProcessVersionStatus status) throws Exception {
        version.setStatus(status);
        assertThat(service.requireContext(scope)).isSameAs(version);
        assertThatThrownBy(() -> service.deleteNode(scope, 10)).isInstanceOf(ResponseException.class).hasMessageContaining("Entwurfsstatus");
        verifyNoInteractions(nodeService, auditLogs);
    }

    @Test
    void rejectsNodesFromOtherProcessOrVersion() {
        node.setProcessVersion(3);
        assertThatThrownBy(() -> service.getNode(scope, 10)).isInstanceOf(ResponseException.class);
        assertThatThrownBy(() -> service.deleteNode(scope, 10)).isInstanceOf(ResponseException.class);
        node.setProcessVersion(2).setProcessId(8);
        assertThatThrownBy(() -> service.getNode(scope, 10)).isInstanceOf(ResponseException.class);
        verifyNoInteractions(nodeService, auditLogs);
    }

    @Test
    void rejectsImmutablePropertiesAndDuplicateKeysWithoutMutation() throws Exception {
        assertThatThrownBy(() -> service.updateNode(scope, 10, Map.of("processId", 8), Map.of(), List.of())).isInstanceOf(ResponseException.class);
        when(nodes.findAllByProcessIdAndProcessVersion(7, 2)).thenReturn(List.of(node, node(11, 7, 2).setDataKey("taken")));
        assertThatThrownBy(() -> service.updateNode(scope, 10, Map.of("dataKey", "taken"), Map.of(), List.of())).isInstanceOf(ResponseException.class);
        verify(nodeService, never()).updateForAuthoring(anyInt(), any(), any(), any());
        verifyNoInteractions(auditLogs);
        assertThat(node.getDataKey()).isEqualTo("node10");
    }

    @Test
    void savesIncompleteConfigurationWithWarningsAndPreservesOmittedProperties() throws Exception {
        var problem = new ProcessNodeProblems(node, List.of("Pflichtfeld fehlt"), Map.of(), new DerivedRuntimeElementData());
        when(nodeService.validate(any(), any(), eq(false), eq(actor))).thenReturn(Optional.of(problem));
        when(nodeService.updateForAuthoring(anyInt(), any(), any(), eq(actor))).thenAnswer(i -> ((ProcessNodeEntity) i.getArgument(1)).setSavedWithErrors(true));
        var result = service.updateNode(scope, 10, Map.of("name", "Titel"), Map.of(), List.of());
        assertThat(JsonMapperTestUtils.createMapper().valueToTree(result).path("savedWithErrors").asBoolean()).isTrue();
        verify(nodeService).updateForAuthoring(eq(10), argThat(n -> n.getDescription().equals("Keep") && n.getName().equals("Titel")), eq(node), eq(actor));
        verify(auditLogs, times(1)).create(any());
    }

    @Test
    void propagatesCapacityRejectionWithoutSuccessAudit() throws Exception {
        when(nodeService.create(any())).thenThrow(ResponseException.badRequest("Knotenlimit erreicht"));
        assertThatThrownBy(() -> service.createNode(scope, "counter", 1, "new", null)).isInstanceOf(ResponseException.class);
        verifyNoInteractions(auditLogs);
    }

    @Test
    void validatesPortsOccupiedConnectionsAndScopeButAllowsLoops() throws Exception {
        when(definition.getPorts()).thenReturn(List.of(new ProcessNodePort("next", "Weiter", "")));
        assertThatThrownBy(() -> service.saveEdge(scope, null, 10, 10, "invented")).isInstanceOf(ResponseException.class);
        var existing = new ProcessEdgeEntity().setId(99).setProcessId(7).setProcessVersion(2).setFromNodeId(10).setToNodeId(10).setViaPort("next");
        when(edges.findByFromNodeIdAndViaPort(10, "next")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.saveEdge(scope, null, 10, 10, "next")).isInstanceOf(ResponseException.class);
        verify(edges, never()).save(any());
        when(edges.findById(99)).thenReturn(Optional.of(existing));
        when(edges.save(any())).thenAnswer(i -> i.getArgument(0));
        service.saveEdge(scope, 99, 10, 10, "next");
        verify(auditLogs, times(1)).create(any());
        node.setProcessVersion(3);
        assertThatThrownBy(() -> service.saveEdge(scope, 99, 10, 10, "next")).isInstanceOf(ResponseException.class);
    }

    @Test
    void deletesConnectedEdgesAndEmitsOneAuditWithTheirIds() throws Exception {
        var edge = new ProcessEdgeEntity().setId(99).setProcessId(7).setProcessVersion(2).setFromNodeId(10).setToNodeId(10).setViaPort("next");
        when(edges.findAllByProcessIdAndProcessVersion(7, 2)).thenReturn(List.of(edge));
        when(edges.findById(99)).thenReturn(Optional.of(edge));
        var result = service.deleteNode(scope, 10);
        assertThat(JsonMapperTestUtils.createMapper().valueToTree(result).path("deletedEdgeIds").get(0).asInt()).isEqualTo(99);
        verify(edges).delete(edge);
        verify(nodeService).performDelete(node);
        verify(auditLogs, times(1)).create(argThat(log -> log.getDiff().toString().contains("deletedEdges")));
    }

    private static ProcessNodeEntity node(int id, int processId, int version) {
        return new ProcessNodeEntity().setId(id).setProcessId(processId).setProcessVersion(version).setName("Node")
                .setDescription("Keep").setDataKey("node" + id).setProcessNodeDefinitionKey("counter").setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues()).setOutputMappings(Map.of());
    }
}

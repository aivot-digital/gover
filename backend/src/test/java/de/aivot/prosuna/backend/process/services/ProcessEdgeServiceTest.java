package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ProcessEdgeServiceTest {
    private static final Integer PROCESS_ID = 12;
    private static final Integer PUBLISHED_VERSION = 1;
    private static final Integer DRAFT_VERSION = 2;
    private static final Integer TESTED_DRAFT_VERSION = 3;

    private ProcessEdgeRepository edgeRepository;
    private ProcessEdgeService service;

    @BeforeEach
    void setUp() throws ResponseException {
        edgeRepository = mock(ProcessEdgeRepository.class);
        var nodeService = mock(ProcessNodeService.class);
        var versionRepository = mock(ProcessVersionRepository.class);
        var testClaimRepository = mock(ProcessTestClaimRepository.class);
        service = new ProcessEdgeService(edgeRepository, nodeService, new ProcessVersionDraftGuard(versionRepository, testClaimRepository));

        when(versionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, PUBLISHED_VERSION)))
                .thenReturn(Optional.of(version(PUBLISHED_VERSION, ProcessVersionStatus.Published)));
        when(versionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, DRAFT_VERSION)))
                .thenReturn(Optional.of(version(DRAFT_VERSION, ProcessVersionStatus.Drafted)));
        when(versionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, TESTED_DRAFT_VERSION)))
                .thenReturn(Optional.of(version(TESTED_DRAFT_VERSION, ProcessVersionStatus.Drafted)));
        when(testClaimRepository.existsByProcessIdAndProcessVersion(PROCESS_ID, TESTED_DRAFT_VERSION))
                .thenReturn(true);

        // Nodes 1 and 2 belong to the published version, nodes 3 and 4 to the draft, nodes 5 and 6 to the tested draft.
        when(nodeService.retrieve(1)).thenReturn(Optional.of(node(1, PUBLISHED_VERSION)));
        when(nodeService.retrieve(2)).thenReturn(Optional.of(node(2, PUBLISHED_VERSION)));
        when(nodeService.retrieve(3)).thenReturn(Optional.of(node(3, DRAFT_VERSION)));
        when(nodeService.retrieve(4)).thenReturn(Optional.of(node(4, DRAFT_VERSION)));
        when(nodeService.retrieve(5)).thenReturn(Optional.of(node(5, TESTED_DRAFT_VERSION)));
        when(nodeService.retrieve(6)).thenReturn(Optional.of(node(6, TESTED_DRAFT_VERSION)));
        when(nodeService.resolvePorts(any(ProcessNodeEntity.class))).thenReturn(new ProcessNodePortResolution(List.of(
                new ProcessNodePort("default", "Standard", "Fortsetzung"),
                new ProcessNodePort("other", "Alternative", "Fortsetzung"),
                new ProcessNodePort("unused", "Ungenutzt", "Fortsetzung")
        ), List.of()));

        when(edgeRepository.save(any(ProcessEdgeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_SavesEdgeBetweenNodesOfDraft() throws ResponseException {
        var result = service.create(new ProcessEdgeEntity(99, PROCESS_ID, DRAFT_VERSION, 3, 4, "default"));

        assertNull(result.getId());
        verify(edgeRepository).save(result);
    }

    @Test
    void create_RejectsEdgeInPublishedVersion() {
        assertStatus(HttpStatus.CONFLICT,
                () -> service.create(new ProcessEdgeEntity(null, PROCESS_ID, PUBLISHED_VERSION, 1, 2, "unused")));

        verify(edgeRepository, never()).save(any(ProcessEdgeEntity.class));
    }

    @Test
    void create_RejectsDraftEdgeConnectingNodesOfOtherVersions() {
        // The workflow engine would follow an edge starting at a published node when executing the published version.
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.create(new ProcessEdgeEntity(null, PROCESS_ID, DRAFT_VERSION, 1, 4, "unused")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.create(new ProcessEdgeEntity(null, PROCESS_ID, DRAFT_VERSION, 3, 2, "default")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.create(new ProcessEdgeEntity(null, PROCESS_ID, DRAFT_VERSION, 3, 99, "default")));

        verify(edgeRepository, never()).save(any(ProcessEdgeEntity.class));
    }

    @Test
    void performUpdate_SavesEdgeOfDraft() throws ResponseException {
        var existing = new ProcessEdgeEntity(8, PROCESS_ID, DRAFT_VERSION, 3, 4, "default");

        var result = service.performUpdate(8, new ProcessEdgeEntity(8, PROCESS_ID, DRAFT_VERSION, 4, 3, "other"), existing);

        assertEquals(4, result.getFromNodeId());
        assertEquals(3, result.getToNodeId());
        assertEquals("other", result.getViaPort());
        verify(edgeRepository).save(existing);
    }

    @Test
    void performUpdate_RejectsEdgeOfPublishedVersion() {
        var existing = new ProcessEdgeEntity(7, PROCESS_ID, PUBLISHED_VERSION, 1, 2, "default");

        assertStatus(HttpStatus.CONFLICT, () -> service.performUpdate(
                7,
                new ProcessEdgeEntity(7, PROCESS_ID, PUBLISHED_VERSION, 1, 2, "other"),
                existing
        ));

        assertEquals("default", existing.getViaPort());
        verify(edgeRepository, never()).save(any(ProcessEdgeEntity.class));
    }

    @Test
    void performUpdate_RejectsRedirectingDraftEdgeToNodeOfPublishedVersion() {
        var existing = new ProcessEdgeEntity(8, PROCESS_ID, DRAFT_VERSION, 3, 4, "default");

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.performUpdate(
                8,
                new ProcessEdgeEntity(8, PROCESS_ID, DRAFT_VERSION, 3, 2, "default"),
                existing
        ));

        assertEquals(4, existing.getToNodeId());
        verify(edgeRepository, never()).save(any(ProcessEdgeEntity.class));
    }

    @Test
    void performDelete_RejectsEdgeOfPublishedVersionAndDeletesEdgeOfDraft() throws ResponseException {
        var publishedEdge = new ProcessEdgeEntity(7, PROCESS_ID, PUBLISHED_VERSION, 1, 2, "default");
        var draftEdge = new ProcessEdgeEntity(8, PROCESS_ID, DRAFT_VERSION, 3, 4, "default");

        assertStatus(HttpStatus.CONFLICT, () -> service.performDelete(publishedEdge));
        service.performDelete(draftEdge);

        verify(edgeRepository, never()).delete(publishedEdge);
        verify(edgeRepository).delete(draftEdge);
    }

    @Test
    void create_RejectsEdgeInDraftWithTestClaim() {
        assertStatus(HttpStatus.CONFLICT,
                () -> service.create(new ProcessEdgeEntity(null, PROCESS_ID, TESTED_DRAFT_VERSION, 5, 6, "default")));

        verify(edgeRepository, never()).save(any(ProcessEdgeEntity.class));
    }

    @Test
    void performUpdate_RejectsEdgeOfDraftWithTestClaim() {
        var existing = new ProcessEdgeEntity(9, PROCESS_ID, TESTED_DRAFT_VERSION, 5, 6, "default");

        assertStatus(HttpStatus.CONFLICT, () -> service.performUpdate(
                9,
                new ProcessEdgeEntity(9, PROCESS_ID, TESTED_DRAFT_VERSION, 6, 5, "other"),
                existing
        ));

        assertEquals(5, existing.getFromNodeId());
        assertEquals("default", existing.getViaPort());
        verify(edgeRepository, never()).save(any(ProcessEdgeEntity.class));
    }

    @Test
    void performDelete_RejectsEdgeOfDraftWithTestClaim() {
        var testedEdge = new ProcessEdgeEntity(9, PROCESS_ID, TESTED_DRAFT_VERSION, 5, 6, "default");

        assertStatus(HttpStatus.CONFLICT, () -> service.performDelete(testedEdge));

        verify(edgeRepository, never()).delete(any(ProcessEdgeEntity.class));
    }

    @Test
    void acceptsDeadlineEdgesOnlyWhileTheSourceDeadlineIsStrict() throws Exception {
        try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var resolver = new ProcessNodePortResolver(validators.getValidator());
            var repository = mock(ProcessEdgeRepository.class);
            var nodes = mock(ProcessNodeService.class);
            var service = new ProcessEdgeService(repository, nodes, mock(ProcessVersionDraftGuard.class));
            var source = new ProcessNodeEntity().setId(1).setProcessId(10).setProcessVersion(2).setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, false)));
            var target = new ProcessNodeEntity().setId(2).setProcessId(10).setProcessVersion(2);
            @SuppressWarnings("unchecked")
            var provider = (ProcessNodeDefinition<Object>) mock(ProcessNodeDefinition.class);
            when(provider.getPorts(any())).thenReturn(List.of());
            when(provider.getDeadlines(any())).thenReturn(List.of(new ProcessNodeDeadlineDefinition("processing", "Bearbeitung", "Bearbeitungsfrist", new ProcessNodePort("processing_overdue", "Fristüberschreitung", "Fortsetzung"))));
            when(nodes.retrieve(1)).thenReturn(Optional.of(source));
            when(nodes.retrieve(2)).thenReturn(Optional.of(target));
            when(nodes.resolvePorts(source)).thenAnswer(invocation -> resolver.resolve(source, provider, new Object()));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            var edge = new ProcessEdgeEntity(null, 10, 2, 1, 2, "processing_overdue");

            assertThrows(ResponseException.class, () -> service.create(edge));
            verify(repository, never()).save(any());
            source.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, true)));
            assertSame(edge, service.create(edge));

            clearInvocations(repository);
            source.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, false)));
            assertThrows(ResponseException.class, () -> service.performUpdate(5, edge, new ProcessEdgeEntity()));
            verify(repository, never()).save(any());
            service.performDelete(edge);
            verify(repository).delete(edge);
        }
    }

    @Test
    void acceptsOnlyCurrentlyAvailablePortsWithinTheSameProcessVersion() throws Exception {
        var repository = mock(ProcessEdgeRepository.class);
        var nodes = mock(ProcessNodeService.class);
        var service = new ProcessEdgeService(repository, nodes, mock(ProcessVersionDraftGuard.class));
        var source = new ProcessNodeEntity().setId(1).setProcessId(10).setProcessVersion(2);
        var target = new ProcessNodeEntity().setId(2).setProcessId(10).setProcessVersion(2);
        when(nodes.retrieve(1)).thenReturn(Optional.of(source));
        when(nodes.retrieve(2)).thenReturn(Optional.of(target));
        when(nodes.resolvePorts(source)).thenReturn(new ProcessNodePortResolution(
                List.of(new ProcessNodePort("dynamic", "Dynamisch", "Fortsetzung")), List.of()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var edge = new ProcessEdgeEntity(null, 10, 2, 1, 2, "dynamic");
        assertSame(edge, service.create(edge));
        clearInvocations(repository);

        edge.setViaPort("removed");
        assertThrows(ResponseException.class, () -> service.create(edge));
        edge.setViaPort("dynamic");
        target.setProcessVersion(3);
        assertThrows(ResponseException.class, () -> service.create(edge));
        target.setProcessVersion(2).setProcessId(11);
        assertThrows(ResponseException.class, () -> service.performUpdate(5, edge, new ProcessEdgeEntity()));
        verify(repository, never()).save(any());
        service.performDelete(edge);
        verify(repository).delete(edge);
    }

    @Test
    void copiesStoredConnectionsWithoutRequiringCurrentlyAvailablePorts() {
        var repository = mock(ProcessEdgeRepository.class);
        var nodes = mock(ProcessNodeService.class);
        var service = new ProcessEdgeService(repository, nodes, mock(ProcessVersionDraftGuard.class));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var edge = new ProcessEdgeEntity(4, 10, 3, 1, 2, "removed");

        assertSame(edge, service.copy(edge));

        assertNull(edge.getId());
        verify(repository).save(edge);
        verifyNoInteractions(nodes);
    }

    @Test
    void refusesUnresolvedAndAlreadyConnectedPorts() throws Exception {
        var repository = mock(ProcessEdgeRepository.class);
        var nodes = mock(ProcessNodeService.class);
        var service = new ProcessEdgeService(repository, nodes, mock(ProcessVersionDraftGuard.class));
        var node = new ProcessNodeEntity().setId(1).setProcessId(10).setProcessVersion(2);
        when(nodes.retrieve(any(Integer.class))).thenReturn(Optional.of(node));
        when(nodes.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(), List.of("Ungültige Konfiguration")));
        var edge = new ProcessEdgeEntity(null, 10, 2, 1, 2, "dynamic");
        assertThrows(ResponseException.class, () -> service.create(edge));
        when(nodes.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(new ProcessNodePort("dynamic", "Ausgang", "Fortsetzung")), List.of()));
        when(repository.findByFromNodeIdAndViaPort(1, "dynamic")).thenReturn(Optional.of(new ProcessEdgeEntity(4, 10, 2, 1, 3, "dynamic")));
        assertThrows(ResponseException.class, () -> service.create(edge));
        verify(repository, never()).save(any());
    }

    private static void assertStatus(HttpStatus status, Executable executable) {
        var exception = assertThrows(ResponseException.class, executable);
        assertEquals(status, exception.getStatus());
    }

    private static ProcessVersionEntity version(Integer processVersion, ProcessVersionStatus status) {
        return new ProcessVersionEntity()
                .setProcessId(PROCESS_ID)
                .setProcessVersion(processVersion)
                .setStatus(status);
    }

    private static ProcessNodeEntity node(Integer id, Integer processVersion) {
        return new ProcessNodeEntity()
                .setId(id)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(processVersion);
    }
}

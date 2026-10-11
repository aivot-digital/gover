package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessEdgeServiceTest {
    private static final Integer PROCESS_ID = 12;
    private static final Integer PUBLISHED_VERSION = 1;
    private static final Integer DRAFT_VERSION = 2;

    private ProcessEdgeRepository edgeRepository;
    private ProcessEdgeService service;

    @BeforeEach
    void setUp() {
        edgeRepository = mock(ProcessEdgeRepository.class);
        var nodeRepository = mock(ProcessNodeRepository.class);
        var versionRepository = mock(ProcessVersionRepository.class);
        service = new ProcessEdgeService(edgeRepository, nodeRepository, new ProcessVersionDraftGuard(versionRepository));

        when(versionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, PUBLISHED_VERSION)))
                .thenReturn(Optional.of(version(PUBLISHED_VERSION, ProcessVersionStatus.Published)));
        when(versionRepository.findById(ProcessVersionEntityId.of(PROCESS_ID, DRAFT_VERSION)))
                .thenReturn(Optional.of(version(DRAFT_VERSION, ProcessVersionStatus.Drafted)));

        // Nodes 1 and 2 belong to the published version, nodes 3 and 4 to the draft.
        when(nodeRepository.findById(1)).thenReturn(Optional.of(node(1, PUBLISHED_VERSION)));
        when(nodeRepository.findById(2)).thenReturn(Optional.of(node(2, PUBLISHED_VERSION)));
        when(nodeRepository.findById(3)).thenReturn(Optional.of(node(3, DRAFT_VERSION)));
        when(nodeRepository.findById(4)).thenReturn(Optional.of(node(4, DRAFT_VERSION)));

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

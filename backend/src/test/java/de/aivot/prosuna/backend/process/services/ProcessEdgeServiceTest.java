package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ProcessEdgeServiceTest {
    @Test
    void acceptsDeadlineEdgesOnlyWhileTheSourceDeadlineIsStrict() throws Exception {
        try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var resolver = new ProcessNodePortResolver(validators.getValidator());
            var repository = mock(ProcessEdgeRepository.class);
            var nodes = mock(ProcessNodeService.class);
            var service = new ProcessEdgeService(repository, nodes);
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
        var service = new ProcessEdgeService(repository, nodes);
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
        var service = new ProcessEdgeService(repository, nodes);
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
        var service = new ProcessEdgeService(repository, nodes);
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
}

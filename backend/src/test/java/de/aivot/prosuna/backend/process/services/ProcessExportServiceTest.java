package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProcessExportServiceTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void processExportPreservesStrictnessThroughJsonImport(boolean strict) throws Exception {
        var processes = mock(ProcessService.class);
        var versions = mock(ProcessVersionService.class);
        var nodes = mock(ProcessNodeService.class);
        var edges = mock(ProcessEdgeService.class);
        var definitions = mock(ProcessNodeDefinitionService.class);
        var definition = mock(ProcessNodeDefinition.class);
        var node = new ProcessNodeEntity().setId(1).setProcessId(10).setProcessVersion(2)
                .setProcessNodeDefinitionKey("test.node").setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues()).setOutputMappings(Map.of())
                .setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, strict)));
        when(processes.retrieve(10)).thenReturn(Optional.of(new ProcessEntity()));
        when(versions.retrieve(ProcessVersionEntityId.of(10, 2))).thenReturn(Optional.of(new ProcessVersionEntity()));
        when(nodes.list(any(ProcessNodeFilter.class))).thenReturn(new PageImpl<>(List.of(node)));
        when(edges.list(any(ProcessDefinitionEdgeFilter.class))).thenReturn(new PageImpl<>(List.of()));
        when(definitions.getProcessNodeDefinition("test.node", 1)).thenReturn(Optional.of(definition));
        when(definition.cleanConfigurationForExport(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new ProcessExportService(processes, versions, nodes, edges, definitions,
                new BuildProperties(), mock(SystemConfigService.class, RETURNS_DEEP_STUBS));

        var exported = service.export(10, 2);
        var mapper = JsonMapperFactory.getInstance();
        var imported = mapper.readValue(mapper.writeValueAsString(exported), ProcessExportService.ProcessExport.class);

        assertEquals(strict, imported.nodes().getFirst().getDeadlines().get("processing").strict());
        assertEquals(3, imported.nodes().getFirst().getDeadlines().get("processing").days());
    }
}

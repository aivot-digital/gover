package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.process.dtos.ProcessNodeResponseDTO;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.services.ProcessNodePortResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessNodeResponseDTOTest {
    @Test
    void preservesExistingFieldsAndAddsPortsOnlyToReadResponses() {
        var node = new ProcessNodeEntity().setId(1).setProcessId(2).setProcessVersion(3)
                .setName("Prüfung").setDataKey("test").setProcessNodeDefinitionKey("test.node")
                .setProcessNodeDefinitionVersion(1).setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of()).setTimeLimitDays(1);
        node.prePersist();
        var resolution = new ProcessNodePortResolution(List.of(ProcessNodePortResolver.DEADLINE_PORT), List.of());
        var mapper = JsonMapperFactory.getInstance();
        var authored = mapper.convertValue(node, Map.class);
        var response = mapper.convertValue(ProcessNodeResponseDTO.of(node, resolution), Map.class);

        authored.forEach((key, value) -> assertEquals(value, response.get(key), "Changed existing field: " + key));
        assertEquals("Prüfung", response.get("displayName"));
        assertTrue(response.containsKey("ports"));
        assertEquals(List.of(), response.get("portResolutionProblems"));
        assertFalse(authored.containsKey("ports"));
        assertFalse(authored.containsKey("portResolutionProblems"));
    }
}

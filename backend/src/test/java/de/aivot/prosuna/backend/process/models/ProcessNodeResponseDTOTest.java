package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.process.dtos.ProcessNodeResponseDTO;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.converters.ProcessNodeDeadlinesConverter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessNodeResponseDTOTest {
    @Test
    void taskDeadlineKeySurvivesApiSerialization() {
        var mapper = JsonMapperFactory.getInstance();
        var task = new de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity()
                .setDeadline(java.time.Instant.parse("2030-05-01T10:00:00Z")).setDeadlineKey("formSubmission");
        var restored = mapper.readValue(mapper.writeValueAsString(task), task.getClass());
        assertEquals(task.getDeadline(), restored.getDeadline());
        assertEquals("formSubmission", restored.getDeadlineKey());
    }

    @Test
    void preservesExistingFieldsAndAddsPortsOnlyToReadResponses() {
        var node = new ProcessNodeEntity().setId(1).setProcessId(2).setProcessVersion(3)
                .setName("Prüfung").setDataKey("test").setProcessNodeDefinitionKey("test.node")
                .setProcessNodeDefinitionVersion(1).setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of()).setDeadlines(Map.of("phase", new ProcessNodeDeadlineConfiguration(1, true)));
        node.prePersist();
        var resolution = new ProcessNodePortResolution(List.of(new ProcessNodePort("phase_overdue", "Fristüberschreitung", "Fortsetzung", "phase")), List.of());
        var mapper = JsonMapperFactory.getInstance();
        var authored = mapper.convertValue(node, Map.class);
        var response = mapper.convertValue(ProcessNodeResponseDTO.of(node, resolution), Map.class);

        authored.forEach((key, value) -> assertEquals(value, response.get(key), "Changed existing field: " + key));
        assertEquals("Prüfung", response.get("displayName"));
        assertEquals(authored.get("deadlines"), response.get("deadlines"));
        assertTrue(response.containsKey("deadlineDefinitions"));
        assertFalse(authored.containsKey("deadlineDefinitions"));
        assertTrue(response.containsKey("ports"));
        assertEquals(List.of(), response.get("portResolutionProblems"));
        assertFalse(authored.containsKey("ports"));
        assertFalse(authored.containsKey("portResolutionProblems"));
    }

    @Test
    void emptyDeadlinesDefaultToNoneAndTypedSettingsRoundTripThroughJsonAndDatabaseConverter() {
        var mapper = JsonMapperFactory.getInstance();
        assertTrue(mapper.readValue("{}", ProcessNodeEntity.class).getDeadlines().isEmpty());
        var converter = new ProcessNodeDeadlinesConverter(mapper);
        var settings = Map.of("message", new ProcessNodeDeadlineConfiguration(3, true),
                "customer", new ProcessNodeDeadlineConfiguration(7, false),
                "optional", new ProcessNodeDeadlineConfiguration(null, false));
        var node = new ProcessNodeEntity().setDeadlines(settings);
        var restored = mapper.readValue(mapper.writeValueAsString(node), ProcessNodeEntity.class);
        assertEquals(settings, restored.getDeadlines());
        assertEquals(settings, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(settings)));
        assertFalse(mapper.readValue("{\"days\":3}", ProcessNodeDeadlineConfiguration.class).strict());
    }
}

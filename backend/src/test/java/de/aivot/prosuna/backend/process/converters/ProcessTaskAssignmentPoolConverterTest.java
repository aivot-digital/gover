package de.aivot.prosuna.backend.process.converters;

import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.prosuna.backend.process.models.ProcessTaskAssignmentPool;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessTaskAssignmentPoolConverterTest {
    private final ProcessTaskAssignmentPoolConverter converter = new ProcessTaskAssignmentPoolConverter(JsonMapper.builder().build());

    @Test
    void roundTripsThePoolAndKeepsMissingPoolsEmpty() {
        var pool = new ProcessTaskAssignmentPool(
                List.of(new DomainAndUserSelectInputElementValue("team", "20"), new DomainAndUserSelectInputElementValue("user", "user-1")),
                List.of("process_instance.read", "process_instance.edit_task")
        );

        assertEquals(pool, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(pool)));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}

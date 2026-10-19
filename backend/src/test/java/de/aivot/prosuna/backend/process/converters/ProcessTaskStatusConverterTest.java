package de.aivot.prosuna.backend.process.converters;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessTaskStatusConverterTest {
    private final ProcessTaskStatusConverter converter = new ProcessTaskStatusConverter();

    @Test
    void convertsStatusesUsingStableDatabaseValues() {
        var expectedValues = Map.ofEntries(
                Map.entry(ProcessTaskStatus.Running, 0),
                Map.entry(ProcessTaskStatus.Paused, 1),
                Map.entry(ProcessTaskStatus.AwaitingPayment, 6),
                Map.entry(ProcessTaskStatus.AwaitingCustomer, 7),
                Map.entry(ProcessTaskStatus.Completed, 2),
                Map.entry(ProcessTaskStatus.Aborted, 3),
                Map.entry(ProcessTaskStatus.Failed, 4),
                Map.entry(ProcessTaskStatus.Restarted, 5),
                Map.entry(ProcessTaskStatus.InProgress, 8),
                Map.entry(ProcessTaskStatus.AwaitingStaff, 9),
                Map.entry(ProcessTaskStatus.DeadlineExceeded, 10)
        );

        assertEquals(ProcessTaskStatus.values().length, expectedValues.size());
        expectedValues.forEach((status, databaseValue) -> {
            assertEquals(databaseValue.shortValue(), converter.convertToDatabaseColumn(status));
            assertEquals(status, converter.convertToEntityAttribute(databaseValue.shortValue()));
        });
    }

    @Test
    void preservesNullValues() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void rejectsUnknownDatabaseValues() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute((short) 99)
        );

        assertEquals("Unknown process task status database value: 99", exception.getMessage());
    }

    @Test
    void keepsEnumNamesForJsonSerialization() throws JacksonException {
        var objectMapper = JsonMapperFactory.getInstance();

        assertEquals("\"Completed\"", objectMapper.writeValueAsString(ProcessTaskStatus.Completed));
        assertEquals(
                ProcessTaskStatus.Completed,
                objectMapper.readValue("\"Completed\"", ProcessTaskStatus.class)
        );
    }
}

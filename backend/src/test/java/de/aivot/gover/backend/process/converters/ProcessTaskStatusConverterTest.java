package de.aivot.gover.backend.process.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessTaskStatusConverterTest {
    private final ProcessTaskStatusConverter converter = new ProcessTaskStatusConverter();

    @Test
    void convertsStatusesUsingStableDatabaseValues() {
        var expectedValues = Map.of(
                ProcessTaskStatus.Running, 0,
                ProcessTaskStatus.Paused, 1,
                ProcessTaskStatus.AwaitingPayment, 6,
                ProcessTaskStatus.AwaitingCustomer, 7,
                ProcessTaskStatus.Completed, 2,
                ProcessTaskStatus.Aborted, 3,
                ProcessTaskStatus.Failed, 4,
                ProcessTaskStatus.Restarted, 5
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
    void keepsEnumNamesForJsonSerialization() throws JsonProcessingException {
        var objectMapper = ObjectMapperFactory.getInstance();

        assertEquals("\"Completed\"", objectMapper.writeValueAsString(ProcessTaskStatus.Completed));
        assertEquals(
                ProcessTaskStatus.Completed,
                objectMapper.readValue("\"Completed\"", ProcessTaskStatus.class)
        );
    }
}

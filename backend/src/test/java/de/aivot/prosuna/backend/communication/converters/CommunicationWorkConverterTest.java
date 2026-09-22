package de.aivot.prosuna.backend.communication.converters;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CommunicationWorkConverterTest {
    private final CommunicationWorkConverter converter = new CommunicationWorkConverter(JsonMapperTestUtils.createMapper());

    @Test
    void emptyOutboxIsSqlNullInBothDirections() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void pendingWorkSurvivesDatabaseRoundTrip() {
        var work = Map.<String, Object>of("nextNodeId", 7, "communicationDeliveryId", "delivery");
        assertEquals(work, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(work)));
    }
}

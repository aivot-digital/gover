package de.aivot.prosuna.backend.customLink.converters;

import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomLinkTypeConverterTest {
    private final CustomLinkTypeConverter converter = new CustomLinkTypeConverter();

    @Test
    void shouldPersistDashboardWithStableValue() {
        assertEquals((short) 0, converter.convertToDatabaseColumn(CustomLinkType.Dashboard));
        assertEquals(CustomLinkType.Dashboard, converter.convertToEntityAttribute((short) 0));
    }

    @Test
    void shouldPreserveNullValues() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void shouldRejectUnknownDatabaseValues() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute((short) 99));
    }
}

package de.aivot.prosuna.backend.config.dtos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemConfigRequestDtoTest {
    @ParameterizedTest
    @ValueSource(strings = {"true", "false", ""})
    void shouldPreserveSerializedValue(String value) {
        var request = new SystemConfigRequestDto(value, null);

        var entity = request.toEntity();

        assertEquals(value, entity.getValue());
    }
}

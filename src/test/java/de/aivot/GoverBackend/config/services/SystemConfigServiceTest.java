package de.aivot.GoverBackend.config.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigServiceTest {
    @Test
    void getValueShouldReturnParsedStoredValue() throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        var service = new SystemConfigService(repository, List.of(new BooleanSystemConfigDefinition()));

        when(repository.findById(BooleanSystemConfigDefinition.KEY))
                .thenReturn(Optional.of(new SystemConfigEntity()
                        .setKey(BooleanSystemConfigDefinition.KEY)
                        .setValue("true")
                        .setPublicConfig(false)));

        var value = service.getValue(BooleanSystemConfigDefinition.KEY);

        assertEquals(true, value);
    }

    @Test
    void getValueShouldReturnDefaultValueWhenStoredValueIsMissing() throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        var service = new SystemConfigService(repository, List.of(new BooleanSystemConfigDefinition()));

        when(repository.findById(BooleanSystemConfigDefinition.KEY))
                .thenReturn(Optional.empty());

        var value = service.getValue(BooleanSystemConfigDefinition.KEY);

        assertEquals(false, value);
    }

    @Test
    void getValueShouldRejectUnknownKey() {
        var repository = mock(SystemConfigRepository.class);
        var service = new SystemConfigService(repository, List.of(new BooleanSystemConfigDefinition()));

        var exception = assertThrows(ResponseException.class, () -> service.getValue("unknown.config"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private static class BooleanSystemConfigDefinition implements SystemConfigDefinition<Boolean> {
        private static final String KEY = "test.boolean";

        @Nonnull
        @Override
        public String getKey() {
            return KEY;
        }

        @Nonnull
        @Override
        public BaseElement getConfigElement() {
            return new CheckboxInputElement();
        }

        @Nonnull
        @Override
        public String getCategory() {
            return "Test";
        }

        @Nonnull
        @Override
        public String getLabel() {
            return "Boolean config";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Boolean config for tests";
        }

        @Nonnull
        @Override
        public String serializeValueToDB(Boolean value) {
            return value == null ? Boolean.FALSE.toString() : value.toString();
        }

        @Override
        public Boolean parseValueFromDB(@Nonnull String value) throws ResponseException {
            return Boolean.parseBoolean(value);
        }
    }
}

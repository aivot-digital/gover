package de.aivot.prosuna.backend.asset.configs;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultStorageAssetsSystemConfigDefinitionTest {
    @Test
    void getValueShouldBeUnsetWithoutStoredValue() throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        when(repository.findById(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY))
                .thenReturn(Optional.empty());
        var service = createService(repository);

        assertNull(service.getValue(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY));
    }

    @Test
    void saveShouldSelectFirstDefaultProviderWithoutStoredValue() throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        when(repository.findById(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY))
                .thenReturn(Optional.empty());
        var service = createService(repository);
        var entity = new SystemConfigEntity()
                .setValue("2");

        service.save(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY, entity);

        verify(repository).save(entity);
    }

    @Test
    void parseValueFromDBShouldRejectNonNumericValue() {
        var definition = new DefaultStorageAssetsSystemConfigDefinition(mock(StorageProviderRepository.class));

        var exception = assertThrows(ResponseException.class, () -> definition.parseValueFromDB("assets"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
    }

    private static SystemConfigService createService(SystemConfigRepository repository) {
        return new SystemConfigService(repository, List.of(
                new DefaultStorageAssetsSystemConfigDefinition(mock(StorageProviderRepository.class))
        ));
    }
}

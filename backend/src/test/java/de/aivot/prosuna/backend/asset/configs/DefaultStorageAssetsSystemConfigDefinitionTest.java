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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void validateChangeShouldRejectReadOnlyProvider() {
        var storageProviderRepository = mock(StorageProviderRepository.class);
        when(storageProviderRepository.existsByIdAndReadOnlyStorageTrue(2)).thenReturn(true);
        var definition = new DefaultStorageAssetsSystemConfigDefinition(storageProviderRepository);

        var exception = assertThrows(ResponseException.class, () -> definition.validateChange("1", "2", true));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void validateChangeShouldAllowWritableProvider() {
        var storageProviderRepository = mock(StorageProviderRepository.class);
        when(storageProviderRepository.existsByIdAndReadOnlyStorageTrue(2)).thenReturn(false);
        var definition = new DefaultStorageAssetsSystemConfigDefinition(storageProviderRepository);

        assertDoesNotThrow(() -> definition.validateChange("1", "2", false));
    }

    @Test
    void validateChangeShouldIgnoreUnchangedValue() {
        var storageProviderRepository = mock(StorageProviderRepository.class);
        var definition = new DefaultStorageAssetsSystemConfigDefinition(storageProviderRepository);

        assertDoesNotThrow(() -> definition.validateChange("2", "2", false));
        verifyNoInteractions(storageProviderRepository);
    }

    private static SystemConfigService createService(SystemConfigRepository repository) {
        return new SystemConfigService(repository, List.of(
                new DefaultStorageAssetsSystemConfigDefinition(mock(StorageProviderRepository.class))
        ));
    }
}

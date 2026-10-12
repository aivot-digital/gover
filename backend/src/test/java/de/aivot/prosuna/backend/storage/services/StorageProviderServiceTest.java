package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.TestData;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderStatus;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.storage.models.StorageProviderMetadataAttribute;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StorageProviderServiceTest {
    @Mock
    private StorageProviderRepository storageProviderRepository;

    @Mock
    private StorageProviderDefinitionService storageProviderDefinitionService;

    @Mock
    private StorageProviderConfigurationService storageProviderConfigurationService;

    @Mock
    private StorageSyncWorker storageSyncWorker;

    @Mock
    private SystemConfigService systemConfigService;

    private StorageProviderService storageProviderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        storageProviderService = new StorageProviderService(
                storageProviderRepository,
                storageProviderDefinitionService,
                storageProviderConfigurationService,
                storageSyncWorker,
                DataSize.ofMegabytes(10),
                systemConfigService
        );
    }

    @Test
    void performUpdate_SystemProviderResync_DoesNotMutateProtectedFields() throws ResponseException {
        var existingEntity = createProvider()
                .setId(7)
                .setName("System Provider")
                .setDescription("Existing description")
                .setStatus(StorageProviderStatus.Synced)
                .setReadOnlyStorage(false)
                .setTestProvider(false)
                .setConfiguration(TestData.authored("bucket", "existing"))
                .setMaxFileSizeInBytes(1024L)
                .setSystemProvider(true)
                .setMetadataAttributes(List.of(attribute("existing-key", "Existing label")));

        var updatedEntity = createProvider()
                .setId(7)
                .setName("Updated Provider")
                .setDescription("Updated description")
                .setStatus(StorageProviderStatus.SyncPending)
                .setReadOnlyStorage(true)
                .setTestProvider(true)
                .setConfiguration(TestData.authored("bucket", "updated"))
                .setMaxFileSizeInBytes(2048L)
                .setSystemProvider(true)
                .setMetadataAttributes(List.of(attribute("updated-key", "Updated label")));

        when(storageProviderRepository.save(any(StorageProviderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = storageProviderService.performUpdate(existingEntity.getId(), updatedEntity, existingEntity);

        assertSame(existingEntity, result);
        assertEquals("System Provider", result.getName());
        assertEquals("Existing description", result.getDescription());
        assertEquals(StorageProviderStatus.SyncPending, result.getStatus());
        assertEquals(false, result.getReadOnlyStorage());
        assertEquals(false, result.getTestProvider());
        assertEquals(TestData.authored("bucket", "existing"), result.getConfiguration());
        assertEquals(1024L, result.getMaxFileSizeInBytes());
        assertEquals(List.of(attribute("existing-key", "Existing label")), result.getMetadataAttributes());

        verify(storageProviderRepository).save(existingEntity);
        verify(storageSyncWorker).publish(existingEntity.getId());
        verifyNoInteractions(storageProviderDefinitionService, storageProviderConfigurationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void performUpdate_RequestedResyncIsStoredAndPublished() throws ResponseException {
        var existingEntity = createProvider().setId(7).setStatus(StorageProviderStatus.Synced);
        var updatedEntity = createProvider().setId(7).setStatus(StorageProviderStatus.SyncPending);
        var definition = (StorageProviderDefinition<Object>) mock(StorageProviderDefinition.class);
        when(storageProviderDefinitionService.retrieveProviderDefinition("local-disk", 1)).thenReturn(Optional.of(definition));
        when(storageProviderRepository.save(any(StorageProviderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = storageProviderService.performUpdate(existingEntity.getId(), updatedEntity, existingEntity);

        assertEquals(StorageProviderStatus.SyncPending, result.getStatus());
        var order = inOrder(storageProviderRepository, storageSyncWorker);
        order.verify(storageProviderRepository).save(existingEntity);
        order.verify(storageSyncWorker).publish(7);
    }

    @Test
    void performUpdate_SystemProviderWithoutResync_Throws() {
        var existingEntity = createProvider()
                .setId(7)
                .setSystemProvider(true)
                .setStatus(StorageProviderStatus.Synced);
        var updatedEntity = createProvider()
                .setId(7)
                .setName("Updated Provider")
                .setStatus(StorageProviderStatus.SyncFailed)
                .setSystemProvider(true);

        var exception = assertThrows(
                ResponseException.class,
                () -> storageProviderService.performUpdate(existingEntity.getId(), updatedEntity, existingEntity)
        );

        assertEquals("Dieser Speicheranbieter kann nicht bearbeitet werden", exception.getMessage());
        verifyNoInteractions(storageProviderRepository, storageSyncWorker, storageProviderDefinitionService, storageProviderConfigurationService);
    }

    @Test
    void performUpdate_MaxFileSizeExceeded_ReturnsFieldDetails() {
        var existingEntity = createProvider()
                .setId(7)
                .setSystemProvider(true)
                .setStatus(StorageProviderStatus.Synced);
        var updatedEntity = createProvider()
                .setId(7)
                .setSystemProvider(true)
                .setStatus(StorageProviderStatus.SyncPending)
                .setMaxFileSizeInBytes(DataSize.ofMegabytes(11).toBytes());

        var exception = assertThrows(
                ResponseException.class,
                () -> storageProviderService.performUpdate(existingEntity.getId(), updatedEntity, existingEntity)
        );

        var details = assertInstanceOf(DerivedRuntimeElementData.class, exception.getDetails());
        assertEquals(
                "Die maximale Dateigröße des Speicheranbieters darf die systemweite Grenze für Uploads nicht überschreiten. Systemweit gelten 10 Megabyte.",
                details.getElementStates().get("maxFileSizeInBytes").getError()
        );
        verifyNoInteractions(storageProviderRepository, storageSyncWorker, storageProviderDefinitionService, storageProviderConfigurationService);
    }

    private static StorageProviderEntity createProvider() {
        return new StorageProviderEntity()
                .setStorageProviderDefinitionKey("local-disk")
                .setStorageProviderDefinitionVersion(1)
                .setName("Provider")
                .setDescription("Description")
                .setType(StorageProviderType.Assets)
                .setStatus(StorageProviderStatus.Synced)
                .setReadOnlyStorage(false)
                .setConfiguration(TestData.authored("bucket", "default"))
                .setMaxFileSizeInBytes(1024L)
                .setSystemProvider(false)
                .setTestProvider(false)
                .setMetadataAttributes(List.of(attribute("default-key", "Default label")));
    }

    private static StorageProviderMetadataAttribute attribute(String key, String label) {
        return new StorageProviderMetadataAttribute()
                .setKey(key)
                .setLabel(label)
                .setDescription(label + " description");
    }
}

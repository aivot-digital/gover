package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.TestData;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.models.StorageProviderMetadataAttribute;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.unit.DataSize;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private RabbitTemplate rabbitTemplate;

    private StorageProviderService storageProviderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        storageProviderService = new StorageProviderService(
                storageProviderRepository,
                storageProviderDefinitionService,
                storageProviderConfigurationService,
                rabbitTemplate,
                DataSize.ofMegabytes(10)
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
        verify(rabbitTemplate).convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, existingEntity.getId());
        verifyNoInteractions(storageProviderDefinitionService, storageProviderConfigurationService);
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
        verifyNoInteractions(storageProviderRepository, rabbitTemplate, storageProviderDefinitionService, storageProviderConfigurationService);
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

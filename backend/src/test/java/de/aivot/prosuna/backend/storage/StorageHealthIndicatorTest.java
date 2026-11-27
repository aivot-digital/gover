package de.aivot.prosuna.backend.storage;

import de.aivot.prosuna.backend.TestData;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderStatus;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.exceptions.StorageException;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import de.aivot.prosuna.backend.storage.services.StorageProviderConfigurationService;
import de.aivot.prosuna.backend.storage.services.StorageProviderDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.health.contributor.Status;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageHealthIndicatorTest {
    @Mock
    private StorageProviderRepository storageProviderRepository;

    @Mock
    private StorageProviderDefinitionService storageProviderDefinitionService;

    @Mock
    private StorageProviderConfigurationService storageProviderConfigurationService;

    @Mock
    private SystemConfigService systemConfigService;

    private StorageProviderDefinition<?> definition;

    private StorageHealthIndicator indicator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        definition = mock(StorageProviderDefinition.class);
        when(definition.getName()).thenReturn("Lokale Festplatte");
        doReturn(Optional.of(definition))
                .when(storageProviderDefinitionService)
                .retrieveProviderDefinition("local-disk", 1);

        indicator = new StorageHealthIndicator(
                storageProviderRepository,
                storageProviderDefinitionService,
                storageProviderConfigurationService,
                systemConfigService
        );
    }

    @Test
    void health_IsUnknownWithHintWithoutDefaultAttachmentStorage() throws Exception {
        when(storageProviderRepository.findAll()).thenReturn(List.of(createProvider(1, "Systemdokumente")));
        when(systemConfigService.getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)).thenReturn(null);

        var health = indicator.health();

        assertEquals(Status.UNKNOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("hint"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void health_IsUpWithReachableDefaultAttachmentStorage() throws Exception {
        when(storageProviderRepository.findAll()).thenReturn(List.of(createProvider(2, "Lokale Vorgangsdokumente")));
        when(systemConfigService.getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)).thenReturn("2");

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void health_IsDownWithUnreachableDefaultAttachmentStorage() throws Exception {
        when(storageProviderRepository.findAll()).thenReturn(List.of(createProvider(2, "Lokale Vorgangsdokumente")));
        when(systemConfigService.getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)).thenReturn("2");
        doThrow(new StorageException("Nicht erreichbar")).when(definition).testConnection(any(), eq(false));

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void health_IsDownWithMissingConfiguredDefaultAttachmentStorage() throws Exception {
        when(storageProviderRepository.findAll()).thenReturn(List.of(createProvider(1, "Systemdokumente")));
        when(systemConfigService.getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)).thenReturn("2");

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }

    @Test
    void health_IsUnknownWithUnreachableOtherStorageWithoutDefaultAttachmentStorage() throws Exception {
        when(storageProviderRepository.findAll()).thenReturn(List.of(createProvider(3, "Externer Speicher")));
        when(systemConfigService.getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)).thenReturn(null);
        doThrow(new StorageException("Nicht erreichbar")).when(definition).testConnection(any(), eq(false));

        var health = indicator.health();

        assertEquals(Status.UNKNOWN, health.getStatus());
    }

    private static StorageProviderEntity createProvider(int id, String name) {
        return new StorageProviderEntity()
                .setId(id)
                .setStorageProviderDefinitionKey("local-disk")
                .setStorageProviderDefinitionVersion(1)
                .setName(name)
                .setDescription("Description")
                .setType(StorageProviderType.Attachments)
                .setStatus(StorageProviderStatus.Synced)
                .setReadOnlyStorage(false)
                .setConfiguration(TestData.authored("root", "./data/"))
                .setMaxFileSizeInBytes(1024L)
                .setSystemProvider(false)
                .setTestProvider(false);
    }
}

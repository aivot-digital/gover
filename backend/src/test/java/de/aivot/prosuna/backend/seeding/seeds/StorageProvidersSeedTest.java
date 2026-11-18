package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.asset.configs.DefaultStorageAssetsSystemConfigDefinition;
import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.plugins.core.v1.storage.LocalDiskStorageProviderDefinitionPropertiesV1;
import de.aivot.prosuna.backend.plugins.core.v1.storage.LocalDiskStorageProviderDefinitionV1;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderStatus;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StorageProvidersSeedTest {
    private static StorageProvidersSeed createSeed(StorageProviderRepository repository,
                                                   SystemConfigRepository systemConfigRepository,
                                                   boolean localStorageProvidersEnabled) {
        var definition = new LocalDiskStorageProviderDefinitionV1(mock(LocalDiskStorageProviderDefinitionPropertiesV1.class), repository);
        return new StorageProvidersSeed(repository, systemConfigRepository, definition, localStorageProvidersEnabled);
    }

    @Test
    void shouldSeedOnlyWithoutExistingStorageProviders() {
        var repository = mock(StorageProviderRepository.class);
        var seed = createSeed(repository, mock(SystemConfigRepository.class), true);

        when(repository.count()).thenReturn(0L);
        assertTrue(seed.shouldSeed());

        when(repository.count()).thenReturn(1L);
        assertFalse(seed.shouldSeed());
    }

    @Test
    void seedShouldCreateLocalProvidersAndUseThemAsDefaults() {
        var repository = mock(StorageProviderRepository.class);
        // IDs differ from a fresh sequence, so the test shows that the configs reference the created providers.
        var nextId = new AtomicInteger(4);
        when(repository.save(any(StorageProviderEntity.class))).thenAnswer(invocation -> {
            StorageProviderEntity entity = invocation.getArgument(0);
            return entity.setId(nextId.getAndIncrement());
        });
        var systemConfigRepository = mock(SystemConfigRepository.class);
        var seed = createSeed(repository, systemConfigRepository, true);

        seed.seed();

        var providers = ArgumentCaptor.forClass(StorageProviderEntity.class);
        verify(repository, times(3)).save(providers.capture());

        var systemProvider = providers.getAllValues().getFirst();
        assertEquals("Systemdokumente", systemProvider.getName());
        assertEquals("./default-assets/", systemProvider.getConfiguration().getLiteral("root"));
        assertTrue(systemProvider.getSystemProvider());
        assertTrue(systemProvider.getReadOnlyStorage());

        var assetsProvider = providers.getAllValues().get(1);
        assertEquals("Lokale Dateien und Medien", assetsProvider.getName());
        assertEquals(StorageProviderType.Assets, assetsProvider.getType());
        assertEquals("./data/assets/", assetsProvider.getConfiguration().getLiteral("root"));
        assertFalse(assetsProvider.getSystemProvider());

        var attachmentsProvider = providers.getAllValues().getLast();
        assertEquals("Lokale Vorgangsanlagen", attachmentsProvider.getName());
        assertEquals(StorageProviderType.Attachments, attachmentsProvider.getType());
        assertEquals("./data/attachments/", attachmentsProvider.getConfiguration().getLiteral("root"));
        assertFalse(attachmentsProvider.getSystemProvider());

        for (var provider : providers.getAllValues()) {
            assertEquals("de.aivot.core.local_disk_storage", provider.getStorageProviderDefinitionKey());
            assertEquals(1, provider.getStorageProviderDefinitionVersion());
            assertEquals(StorageProviderStatus.SyncPending, provider.getStatus());
            assertEquals(10L * 1024 * 1024, provider.getMaxFileSizeInBytes());
        }

        var configs = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(systemConfigRepository, times(2)).save(configs.capture());
        assertEquals(
                Map.of(
                        DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY, "5",
                        DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY, "6"
                ),
                configs.getAllValues().stream().collect(Collectors.toMap(SystemConfigEntity::getKey, SystemConfigEntity::getValue))
        );
        assertEquals(List.of(false, false), configs.getAllValues().stream().map(SystemConfigEntity::getPublicConfig).toList());
    }

    @Test
    void seedShouldOnlyCreateSystemProviderWhenLocalProvidersAreDisabled() {
        var repository = mock(StorageProviderRepository.class);
        when(repository.save(any(StorageProviderEntity.class))).thenAnswer(invocation -> {
            StorageProviderEntity entity = invocation.getArgument(0);
            return entity.setId(1);
        });
        var systemConfigRepository = mock(SystemConfigRepository.class);
        var seed = createSeed(repository, systemConfigRepository, false);

        seed.seed();

        var providers = ArgumentCaptor.forClass(StorageProviderEntity.class);
        verify(repository).save(providers.capture());
        assertEquals("Systemdokumente", providers.getValue().getName());
        assertTrue(providers.getValue().getSystemProvider());
        verifyNoInteractions(systemConfigRepository);
    }
}

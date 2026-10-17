package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.asset.configs.DefaultStorageAssetsSystemConfigDefinition;
import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.plugins.core.v1.storage.LocalDiskStorageProviderDefinitionV1;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.seeding.models.SeedProvider;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderStatus;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the local storage providers of a fresh installation. The providers are saved through the repository, because
 * the storage provider service only accepts roots below {@code storage.local.v1.allowed-local-roots}, and the system
 * documents are located outside of them. The storage startup service synchronizes the seeded providers once the
 * application is ready.
 * <p>
 * The system documents are shipped with the application, so they are always seeded. The local providers for documents,
 * media and process attachments can be disabled for deployments without a local disk shared by all instances, such as
 * Kubernetes. Administrators then create the storage providers and select the defaults themselves.
 */
@Component
public class StorageProvidersSeed implements SeedProvider {
    private static final Logger logger = LoggerFactory.getLogger(StorageProvidersSeed.class);

    private static final long MAX_FILE_SIZE_IN_BYTES = 10 * 1024 * 1024;

    private final StorageProviderRepository storageProviderRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final LocalDiskStorageProviderDefinitionV1 localDiskStorageProviderDefinition;
    private final boolean localStorageProvidersEnabled;

    public StorageProvidersSeed(StorageProviderRepository storageProviderRepository,
                                SystemConfigRepository systemConfigRepository,
                                LocalDiskStorageProviderDefinitionV1 localDiskStorageProviderDefinition,
                                @Value("${prosuna.seeding.local-storage-providers.enabled:true}") boolean localStorageProvidersEnabled) {
        this.storageProviderRepository = storageProviderRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.localDiskStorageProviderDefinition = localDiskStorageProviderDefinition;
        this.localStorageProvidersEnabled = localStorageProvidersEnabled;
    }

    @Override
    public boolean shouldSeed() {
        return storageProviderRepository.count() == 0;
    }

    @Override
    public void seed() {
        storageProviderRepository.save(createLocalDiskProvider(
                "Systemdokumente",
                "Dieser Speicheranbieter beinhaltet die standardmäßig ausgelieferten Dokumente für das System. Er ist schreibgeschützt und kann nicht gelöscht werden.",
                StorageProviderType.Assets,
                "./default-assets/"
        )
                .setReadOnlyStorage(true)
                .setSystemProvider(true));

        if (!localStorageProvidersEnabled) {
            logger.info("Skipping the local storage providers because prosuna.seeding.local-storage-providers.enabled is false");
            return;
        }

        var assetsProvider = storageProviderRepository.save(createLocalDiskProvider(
                "Lokale Dokumente & Medien",
                "Dieser Speicheranbieter ermöglicht das Speichern von Dokumenten und Medien auf dem lokalen Dateisystem des Servers.",
                StorageProviderType.Assets,
                "./data/assets/"
        ));

        var attachmentsProvider = storageProviderRepository.save(createLocalDiskProvider(
                "Lokale Vorgangsdokumente",
                "Dieser Speicheranbieter ermöglicht das Speichern von Vorgangsdokumenten auf dem lokalen Dateisystem des Servers.",
                StorageProviderType.Attachments,
                "./data/attachments/"
        ));

        saveSystemConfig(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY, assetsProvider.getId());
        saveSystemConfig(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY, attachmentsProvider.getId());
    }

    @Nonnull
    private StorageProviderEntity createLocalDiskProvider(@Nonnull String name,
                                                          @Nonnull String description,
                                                          @Nonnull StorageProviderType type,
                                                          @Nonnull String root) {
        return new StorageProviderEntity()
                .setStorageProviderDefinitionKey(localDiskStorageProviderDefinition.getKey())
                .setStorageProviderDefinitionVersion(localDiskStorageProviderDefinition.getMajorVersion())
                .setName(name)
                .setDescription(description)
                .setType(type)
                .setStatus(StorageProviderStatus.SyncPending)
                .setReadOnlyStorage(false)
                .setConfiguration(new AuthoredElementValues().putLiteral("root", root))
                .setMaxFileSizeInBytes(MAX_FILE_SIZE_IN_BYTES)
                .setSystemProvider(false)
                .setTestProvider(false)
                .setMetadataAttributes(List.of());
    }

    private void saveSystemConfig(@Nonnull String key, @Nonnull Integer storageProviderId) {
        // The system config service cannot save these configs on a fresh installation: it validates the change from
        // the empty default value, which is not a valid storage provider ID.
        systemConfigRepository.save(new SystemConfigEntity()
                .setKey(key)
                .setValue(String.valueOf(storageProviderId))
                .setPublicConfig(false));
    }
}

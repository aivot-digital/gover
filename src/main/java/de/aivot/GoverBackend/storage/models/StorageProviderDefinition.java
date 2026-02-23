package de.aivot.GoverBackend.storage.models;

import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.enums.PluginComponentType;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

public interface StorageProviderDefinition<T> extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.StorageProviderDefinition;
    }

    @Nullable
    ConfigLayoutElement getProviderConfigLayout() throws ResponseException;

    Class<T> getConfigClass();

    void initializeProvider(@Nonnull T config) throws StorageException;

    boolean shouldResync(@Nullable T oldConfig, @Nonnull T newConfig);

    void testConnection(@Nonnull T config) throws StorageException;

    @Nonnull
    default StorageFolder rootFolder(@Nonnull T config) throws StorageException {
        return rootFolder(config, false);
    }

    @Nonnull
    default StorageFolder rootFolder(@Nonnull T config, boolean recursive) throws StorageException {
        return retrieveFolder(config, "/", recursive)
                .orElseThrow(() -> new StorageException("Das Stammverzeichnis existiert nicht."));
    }

    @Nonnull
    StorageFolder createFolder(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    @Nonnull
    default Optional<StorageFolder> retrieveFolder(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException {
        return retrieveFolder(config, pathFromRoot, false);
    }

    @Nonnull
    Optional<StorageFolder> retrieveFolder(@Nonnull T config, @Nonnull String pathFromRoot, boolean recursive) throws StorageException;

    boolean folderExists(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    void deleteFolder(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    @Nonnull
    default StorageDocument storeDocument(@Nonnull T config, @Nonnull String pathFromRoot, @Nonnull MultipartFile file) throws StorageException {
        try {
            return storeDocument(config, pathFromRoot, file.getBytes());
        } catch (Exception e) {
            throw new StorageException(
                    e,
                    "Das Dokument %s (%s) konnte nicht zur Speicherung abgelegt werden.",
                    pathFromRoot,
                    file.getOriginalFilename()
            );
        }
    }

    @Nonnull
    StorageDocument storeDocument(@Nonnull T config, @Nonnull String pathFromRoot, @Nonnull byte[] data) throws StorageException;

    @Nonnull
    Optional<StorageDocument> retrieveDocument(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    @Nonnull
    InputStream retrieveDocumentContent(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    boolean documentExists(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    void deleteDocument(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;
}

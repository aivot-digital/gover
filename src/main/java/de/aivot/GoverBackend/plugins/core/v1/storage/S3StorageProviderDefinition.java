package de.aivot.GoverBackend.plugins.core.v1.storage;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;

@Component
public class S3StorageProviderDefinition implements StorageProviderDefinition<S3StorageProviderDefinition.Config>, PluginComponent {
    private final SecretRepository secretRepository;

    public S3StorageProviderDefinition(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "S3 Speicheranbieter";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Speichert Dokumente auf einem S3-kompatiblen Speicher.";
    }

    @Nonnull
    @Override
    public String getKey() {
        return "s3_storage";
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
    }

    @Nullable
    @Override
    public ConfigLayoutElement getProviderConfigLayout() throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e);
        }

        layout
                .findChild("secret_key_secret", SelectInputElement.class)
                .ifPresent(field -> {
                    var options = secretRepository
                            .findAll()
                            .stream()
                            .map(secret -> RadioInputElementOption.of(
                                    secret.getKey().toString(),
                                    secret.getName()
                            ))
                            .toList();

                    field.setOptions(options);
                });

        return layout;
    }

    @Override
    public Class<S3StorageProviderDefinition.Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public void initializeProvider(@Nonnull Config config) throws StorageException {
        // Nothing to do here
    }

    @Override
    public boolean shouldResync(@Nullable Config oldConfig, @Nonnull Config newConfig) {
        if (oldConfig == null) {
            return true;
        }
        return !oldConfig.endpoint.equals(newConfig.endpoint)
                || !oldConfig.bucket.equals(newConfig.bucket)
                || !oldConfig.accessKey.equals(newConfig.accessKey)
                || !oldConfig.secretKeySecret.equals(newConfig.secretKeySecret);
    }

    @Nonnull
    @Override
    public StorageFolder rootFolder(@Nonnull Config config, boolean recursive) throws StorageException {
        return retrieveFolder(config, "/", recursive)
                .orElseThrow(() -> new StorageException("Das Stammverzeichnis existiert nicht."));
    }

    /**
     * Ensures the given path is prefixed with a single '/'.
     */
    private static String toPrefixWithSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    @Nonnull
    @Override
    public StorageFolder createFolder(@Nonnull Config config, @Nonnull String pathFromRoot) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public Optional<StorageFolder> retrieveFolder(@Nonnull Config config, @Nonnull String pathFromRoot, boolean recursive) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private StorageFolder retrieveFolderRecursive(@Nonnull Path rootPathReal,
                                                  @Nonnull Path folderToRetrievePathReal,
                                                  @Nonnull String pathFromRoot) {
        // Extract the name of this folder based on the real path
        var folderToRetrieveName = folderToRetrievePathReal
                .getFileName()
                .toString();

        // Create the folder object, which will be returned
        var folderObject = new StorageFolder(
                toPrefixWithSlash(pathFromRoot),
                folderToRetrieveName,
                null,
                new LinkedList<>(),
                new LinkedList<>(),
                false
        );

        try (var files = Files.list(folderToRetrievePathReal)) {
            files.forEach(childPathReal -> {
                var pathFromRootToChild = rootPathReal.relativize(childPathReal).toString();
                pathFromRootToChild = toPrefixWithSlash(pathFromRootToChild);
                var filename = childPathReal.getFileName().toString();

                if (Files.isDirectory(childPathReal)) {
                    var subfolder = retrieveFolderRecursive(rootPathReal, childPathReal, pathFromRootToChild);
                    folderObject.addSubfolder(subfolder);
                } else if (Files.isRegularFile(childPathReal)) {
                    var document = new StorageDocument(pathFromRootToChild, filename);
                    folderObject.addDocument(document);
                }
            });
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Lesen des Verzeichnisses %s: %s.",
                    StringUtils.quote(folderToRetrievePathReal.toString()),
                    e.getMessage());
        }

        return folderObject;
    }

    /**
     * Retrieve a folder non-recursively, only listing its immediate subfolders and documents.
     *
     * @param rootPathReal             The real root path on the filesystem.
     * @param folderToRetrievePathReal The real folder path to retrieve on the filesystem.
     * @param pathFromRoot             The path from the root to the folder to retrieve.
     * @return The retrieved folder with its immediate subfolders and documents.
     */
    private StorageFolder retrieveFolderFlat(@Nonnull Path rootPathReal,
                                             @Nonnull Path folderToRetrievePathReal,
                                             @Nonnull String pathFromRoot) {
        // Extract the name of this folder based on the real path
        var folderToRetrieveName = folderToRetrievePathReal
                .getFileName()
                .toString();

        // Create the folder object, which will be returned
        var folderObject = new StorageFolder(
                toPrefixWithSlash(pathFromRoot),
                folderToRetrieveName,
                null,
                new LinkedList<>(),
                new LinkedList<>(),
                false
        );

        // Extract the name from the folderToRetrievePath
        var folderName = folderToRetrievePathReal.getFileName().toString();
        folderObject.setName(folderName);

        // List all files and directories in the folder
        var dirFiles = folderToRetrievePathReal
                .toFile()
                .listFiles();

        // If there are no files, return the empty folder object
        if (dirFiles == null) {
            return folderObject;
        }

        // Iterate over all files and directories
        for (var file : dirFiles) {
            // Build the absolute pathFromRoot of this file, relative to the root pathFromRoot
            var pathFromTheRootToThisFile = rootPathReal
                    .relativize(file.toPath())
                    .toString();
            pathFromTheRootToThisFile = toPrefixWithSlash(pathFromTheRootToThisFile);
            // Extract the filename
            var filename =
                    file.getName();

            if (file.isDirectory()) {
                var subfolder = new StorageFolder(
                        pathFromTheRootToThisFile,
                        filename,
                        null,
                        new LinkedList<>(),
                        new LinkedList<>(),
                        false
                );
                folderObject.addSubfolder(subfolder);
            } else if (file.isFile()) {
                var document = new StorageDocument(
                        pathFromTheRootToThisFile,
                        filename
                );
                folderObject.addDocument(document);
            }
        }

        return folderObject;
    }

    @Override
    public boolean folderExists(@Nonnull Config config, @Nonnull String path) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void deleteFolder(@Nonnull Config config, @Nonnull String path) throws StorageException {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public StorageDocument storeDocument(@Nonnull Config config, @Nonnull String path, @Nonnull byte[] data) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public Optional<StorageDocument> retrieveDocument(@Nonnull Config config, @Nonnull String path) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public InputStream retrieveDocumentContent(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public boolean documentExists(@Nonnull Config config, @Nonnull String path) {
        return false;
    }

    @Override
    public void deleteDocument(@Nonnull Config config, @Nonnull String path) {
        // TODO
    }

    @LayoutElementPOJOBinding(id = "config", type = ElementType.ConfigLayout)
    public static class Config {
        @InputElementPOJOBinding(id = "endpoint", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Endpoint"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die URL des S3-kompatiblen Speichers."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String endpoint;

        @InputElementPOJOBinding(id = "bucket", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bucket"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Name des Buckets, in dem die Dateien gespeichert werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String bucket;

        @InputElementPOJOBinding(id = "access_key", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Access Key"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Access Key für den Zugriff auf den S3-kompatiblen Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String accessKey;

        @InputElementPOJOBinding(id = "secret_key_secret", type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Secret Key"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Geheimnis des Secret Keys für den Zugriff auf den S3-kompatiblen Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String secretKeySecret;
    }
}

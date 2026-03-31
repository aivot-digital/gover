package de.aivot.GoverBackend.plugins.core.v1.storage;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Optional;

@Component
public class LocalDiskStorageProviderDefinitionV1 implements StorageProviderDefinition<LocalDiskStorageProviderDefinitionV1.Config> {
    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Lokaler Speicheranbieter";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Speichert Dokumente auf dem lokalen Dateisystem des Servers.";
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "local_disk_storage";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public Boolean getSupportsMetadataAttributes() {
        return false;
    }

    @Nullable
    @Override
    public ConfigLayoutElement getProviderConfigLayout() throws ResponseException {
        try {
            return ElementPOJOMapper.createFromPOJO(Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e);
        }
    }

    @Override
    public Class<LocalDiskStorageProviderDefinitionV1.Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public void initializeProvider(@Nonnull Config config) throws StorageException {
        var rootPathReal = config.getRealRootPath();
        try {
            Files.createDirectories(rootPathReal);
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Initialisieren des Speicheranbieters. Das Stammverzeichnis %s konnte nicht erstellt werden: %s.",
                    StringUtils.quote(rootPathReal.toString()),
                    e.getMessage());
        }
    }

    @Override
    public boolean shouldResync(@Nullable Config oldConfig, @Nonnull Config newConfig) {
        if (oldConfig == null) {
            return true;
        }
        return !oldConfig.getRealRootPath().equals(newConfig.getRealRootPath());
    }

    @Override
    public void testConnection(@Nonnull Config config, @Nonnull Boolean mustCheckWritable) throws StorageException {
        var rootPathReal = config.getRealRootPath();
        if (!Files.exists(rootPathReal) || !Files.isDirectory(rootPathReal) || !Files.isWritable(rootPathReal)) {
            throw new StorageException(
                    "Die Verbindung zum lokalen Dateisystem konnte nicht hergestellt werden. " +
                            "Das Stammverzeichnis %s existiert nicht oder ist nicht beschreibbar.",
                    StringUtils.quote(rootPathReal.toString())
            );
        }
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

    private static String toSuffixWithSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.endsWith("/") ? path : path + "/";
    }

    @Nonnull
    @Override
    public StorageFolder createFolder(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var folderToCreatePathReal = getSecurePath(config.getRealRootPath(), pathFromRoot);

        try {
            Files.createDirectories(folderToCreatePathReal);
            return new StorageFolder(
                    toSuffixWithSlash(toPrefixWithSlash(pathFromRoot)),
                    folderToCreatePathReal.getFileName().toString(),
                    new LinkedList<>(),
                    new LinkedList<>(),
                    false
            );
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Erstellen des Verzeichnisses %s: %s",
                    StringUtils.quote(pathFromRoot),
                    e.getMessage());
        }
    }

    @Nonnull
    @Override
    public Optional<StorageFolder> retrieveFolder(@Nonnull Config config, @Nonnull String pathFromRoot, boolean recursive) throws StorageException {
        // Get the path to the real root directory on the filesystem
        var rootPathReal = config.getRealRootPath();

        // Get the path to the real desired folder on the filesystem
        var folderToRetrievePathReal = getSecurePath(rootPathReal, pathFromRoot);

        // Check if the folder, which should be retrieved, exists
        if (!Files.exists(folderToRetrievePathReal) || !Files.isDirectory(folderToRetrievePathReal)) {
            return Optional.empty();
        }

        StorageFolder folder;
        if (recursive) {
            folder = retrieveFolderRecursive(rootPathReal, folderToRetrievePathReal, pathFromRoot);
        } else {
            folder = retrieveFolderFlat(rootPathReal, folderToRetrievePathReal, pathFromRoot);
        }

        return Optional.of(folder);
    }

    private StorageFolder retrieveFolderRecursive(@Nonnull Path rootPathReal,
                                                  @Nonnull Path folderToRetrievePathReal,
                                                  @Nonnull String pathFromRoot) throws StorageException {
        // Extract the name of this folder based on the real path
        var folderToRetrieveName = folderToRetrievePathReal
                .getFileName()
                .toString();

        // Create the folder object, which will be returned
        var folderObject = new StorageFolder(
                toSuffixWithSlash(toPrefixWithSlash(pathFromRoot)),
                folderToRetrieveName,
                new LinkedList<>(),
                new LinkedList<>(),
                false
        );

        try (var files = Files.list(folderToRetrievePathReal)) {
            var children = files.toList();
            for (var childPathReal : children) {
                var pathFromRootToChild = rootPathReal.relativize(childPathReal).toString();
                pathFromRootToChild = toPrefixWithSlash(pathFromRootToChild);
                var filename = childPathReal.getFileName().toString();

                if (Files.isDirectory(childPathReal)) {
                    var subfolder = retrieveFolderRecursive(rootPathReal, childPathReal, pathFromRootToChild);
                    folderObject.addSubfolder(subfolder);
                } else if (Files.isRegularFile(childPathReal)) {
                    long size;
                    try {
                        size = Files.size(childPathReal);
                    } catch (IOException e) {
                        throw new StorageException(
                                e,
                                "Fehler beim Lesen der Dateigröße für %s: %s.",
                                StringUtils.quote(childPathReal.toString()),
                                e.getMessage()
                        );
                    }
                    var document = new StorageDocument(pathFromRootToChild, filename, size, StorageItemMetadata.empty());
                    folderObject.addDocument(document);
                }
            }
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
                                             @Nonnull String pathFromRoot) throws StorageException {
        // Extract the name of this folder based on the real path
        var folderToRetrieveName = folderToRetrievePathReal
                .getFileName()
                .toString();

        // Create the folder object, which will be returned
        var folderObject = new StorageFolder(
                toSuffixWithSlash(toPrefixWithSlash(pathFromRoot)),
                folderToRetrieveName,
                new LinkedList<>(),
                new LinkedList<>(),
                false
        );

        // Extract the name from the folderToRetrievePath
        var folderName = folderToRetrievePathReal.getFileName().toString();
        folderObject.setName(folderName);

        // List all files and directories in the folder
        try (var dirEntries = Files.list(folderToRetrievePathReal)) {
            for (var childPathReal : dirEntries.toList()) {
                var pathFromTheRootToThisFile = rootPathReal
                        .relativize(childPathReal)
                        .toString();
                pathFromTheRootToThisFile = toPrefixWithSlash(pathFromTheRootToThisFile);
                var filename = childPathReal.getFileName().toString();

                if (Files.isDirectory(childPathReal)) {
                    var subfolder = new StorageFolder(
                            toSuffixWithSlash(pathFromTheRootToThisFile),
                            filename,
                            new LinkedList<>(),
                            new LinkedList<>(),
                            false
                    );
                    folderObject.addSubfolder(subfolder);
                } else if (Files.isRegularFile(childPathReal)) {
                    long size;
                    try {
                        size = Files.size(childPathReal);
                    } catch (IOException e) {
                        throw new StorageException(
                                e,
                                "Fehler beim Lesen der Dateigröße für %s: %s.",
                                StringUtils.quote(childPathReal.toString()),
                                e.getMessage()
                        );
                    }

                    var document = new StorageDocument(
                            pathFromTheRootToThisFile,
                            filename,
                            size,
                            StorageItemMetadata.empty()
                    );
                    folderObject.addDocument(document);
                }
            }
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Lesen des Verzeichnisses %s: %s.",
                    StringUtils.quote(folderToRetrievePathReal.toString()),
                    e.getMessage());
        }

        return folderObject;
    }

    @Override
    public boolean folderExists(@Nonnull Config config, @Nonnull String path) {
        var folderToCheckPathReal = getSecurePath(config.getRealRootPath(), path);
        return Files.exists(folderToCheckPathReal) && Files.isDirectory(folderToCheckPathReal);
    }

    @Nonnull
    @Override
    public StorageFolder moveFolder(@Nonnull Config config,
                                    @Nonnull String sourcePathFromRoot,
                                    @Nonnull String targetPathFromRoot) throws StorageException {
        var sourcePathReal = getSecurePath(config.getRealRootPath(), sourcePathFromRoot);
        var targetPathReal = getSecurePath(config.getRealRootPath(), targetPathFromRoot);

        if (!Files.exists(sourcePathReal) || !Files.isDirectory(sourcePathReal)) {
            throw new StorageException("Der Quellordner %s konnte nicht gefunden werden.", StringUtils.quote(sourcePathFromRoot));
        }

        if (sourcePathReal.equals(targetPathReal)) {
            return retrieveFolder(config, toSuffixWithSlash(toPrefixWithSlash(targetPathFromRoot)), true)
                    .orElseThrow(() -> new StorageException("Der Ordner %s konnte nicht abgerufen werden.", StringUtils.quote(targetPathFromRoot)));
        }

        if (targetPathReal.startsWith(sourcePathReal)) {
            throw new StorageException("Der Zielordner %s darf nicht innerhalb des Quellordners %s liegen.", StringUtils.quote(targetPathFromRoot), StringUtils.quote(sourcePathFromRoot));
        }

        var targetParentDirectoryReal = targetPathReal.getParent();
        if (targetParentDirectoryReal == null || !Files.exists(targetParentDirectoryReal) || !Files.isDirectory(targetParentDirectoryReal)) {
            throw new StorageException("Das Zielverzeichnis für den Ordner %s existiert nicht.", StringUtils.quote(targetPathFromRoot));
        }

        if (Files.exists(targetPathReal)) {
            deleteFolder(config, targetPathFromRoot);
        }

        try {
            Files.move(sourcePathReal, targetPathReal, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Verschieben des Ordners von %s nach %s: %s.",
                    StringUtils.quote(sourcePathFromRoot),
                    StringUtils.quote(targetPathFromRoot),
                    e.getMessage());
        }

        return retrieveFolder(config, toSuffixWithSlash(toPrefixWithSlash(targetPathFromRoot)), true)
                .orElseGet(() -> new StorageFolder(
                        toSuffixWithSlash(toPrefixWithSlash(targetPathFromRoot)),
                        StringUtils.getLastPathSegment(targetPathFromRoot),
                        new LinkedList<>(),
                        new LinkedList<>(),
                        true
                ));
    }

    @Nonnull
    @Override
    public StorageFolder copyFolder(@Nonnull Config config,
                                    @Nonnull String sourcePathFromRoot,
                                    @Nonnull String targetPathFromRoot) throws StorageException {
        var sourcePathReal = getSecurePath(config.getRealRootPath(), sourcePathFromRoot);
        var targetPathReal = getSecurePath(config.getRealRootPath(), targetPathFromRoot);

        if (!Files.exists(sourcePathReal) || !Files.isDirectory(sourcePathReal)) {
            throw new StorageException("Der Quellordner %s konnte nicht gefunden werden.", StringUtils.quote(sourcePathFromRoot));
        }

        if (sourcePathReal.equals(targetPathReal)) {
            return retrieveFolder(config, toSuffixWithSlash(toPrefixWithSlash(targetPathFromRoot)), true)
                    .orElseThrow(() -> new StorageException("Der Ordner %s konnte nicht abgerufen werden.", StringUtils.quote(targetPathFromRoot)));
        }

        if (targetPathReal.startsWith(sourcePathReal)) {
            throw new StorageException("Der Zielordner %s darf nicht innerhalb des Quellordners %s liegen.", StringUtils.quote(targetPathFromRoot), StringUtils.quote(sourcePathFromRoot));
        }

        var targetParentDirectoryReal = targetPathReal.getParent();
        if (targetParentDirectoryReal == null || !Files.exists(targetParentDirectoryReal) || !Files.isDirectory(targetParentDirectoryReal)) {
            throw new StorageException("Das Zielverzeichnis für den Ordner %s existiert nicht.", StringUtils.quote(targetPathFromRoot));
        }

        if (Files.exists(targetPathReal)) {
            deleteFolder(config, targetPathFromRoot);
        }

        try (var sourceEntries = Files.walk(sourcePathReal)) {
            for (var sourceEntry : sourceEntries.toList()) {
                try {
                    var relative = sourcePathReal.relativize(sourceEntry);
                    var targetEntry = targetPathReal.resolve(relative);

                    if (Files.isDirectory(sourceEntry)) {
                        Files.createDirectories(targetEntry);
                    } else {
                        Files.copy(sourceEntry, targetEntry, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new StorageException(e,
                            "Fehler beim Kopieren des Ordners von %s nach %s: %s.",
                            StringUtils.quote(sourcePathFromRoot),
                            StringUtils.quote(targetPathFromRoot),
                            e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Kopieren des Ordners von %s nach %s: %s.",
                    StringUtils.quote(sourcePathFromRoot),
                    StringUtils.quote(targetPathFromRoot),
                    e.getMessage());
        }

        return retrieveFolder(config, toSuffixWithSlash(toPrefixWithSlash(targetPathFromRoot)), true)
                .orElseGet(() -> new StorageFolder(
                        toSuffixWithSlash(toPrefixWithSlash(targetPathFromRoot)),
                        StringUtils.getLastPathSegment(targetPathFromRoot),
                        new LinkedList<>(),
                        new LinkedList<>(),
                        true
                ));
    }

    @Override
    public void deleteFolder(@Nonnull Config config, @Nonnull String path) throws StorageException {
        var folderToDeletePathReal = getSecurePath(config.getRealRootPath(), path);

        try {
            var filesToDelete = Files.walk(folderToDeletePathReal)
                    .map(Path::toFile)
                    .sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                    .toList();

            for (var file : filesToDelete) {
                if (!file.delete()) {
                    throw new StorageException(
                            "Fehler beim Löschen der Datei/Verzeichnis %s.",
                            StringUtils.quote(file.getAbsolutePath())
                    );
                }
            }
        } catch (NoSuchFileException e) {
            // If the folder does not exist, we can consider it as already deleted, so we ignore this exception
            return;
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Löschen des Verzeichnisses %s: %s.",
                    StringUtils.quote(path),
                    e.getMessage());
        }
    }

    @Nonnull
    @Override
    public StorageDocument storeDocument(@Nonnull Config config, @Nonnull String path, @Nonnull InputStream data, @Nonnull StorageItemMetadata metadata) throws StorageException {
        // Check if the parent directory exists
        var documentPathReal = getSecurePath(config.getRealRootPath(), path);
        var parentDirectoryReal = documentPathReal.getParent();
        if (parentDirectoryReal == null || !Files.exists(parentDirectoryReal) || !Files.isDirectory(parentDirectoryReal)) {
            throw new StorageException("Das übergeordnete Verzeichnis für das Dokument %s existiert nicht.", StringUtils.quote(path));
        }

        // Sanitize the filename
        var sanitizedFilename = sanitizeFilename(documentPathReal.getFileName().toString());
        var sanitizedDocumentPathReal = parentDirectoryReal.resolve(sanitizedFilename);

        // Write the data to the file
        try {
            Files.copy(data, sanitizedDocumentPathReal, StandardCopyOption.REPLACE_EXISTING);
            var relativePathFromRoot = config.getRealRootPath().relativize(sanitizedDocumentPathReal).toString();
            relativePathFromRoot = toPrefixWithSlash(relativePathFromRoot);
            var fileSize = Files.size(sanitizedDocumentPathReal);
            return new StorageDocument(relativePathFromRoot, sanitizedFilename, fileSize, StorageItemMetadata.empty());
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Speichern des Dokuments %s: %s.",
                    StringUtils.quote(path),
                    e.getMessage());
        }
    }

    @Nonnull
    @Override
    public Optional<StorageDocument> retrieveDocument(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var documentPathReal = getSecurePath(config.getRealRootPath(), pathFromRoot);
        if (!Files.exists(documentPathReal) || !Files.isRegularFile(documentPathReal)) {
            return Optional.empty();
        }
        var relativePathFromRoot = config.getRealRootPath().relativize(documentPathReal).toString();
        relativePathFromRoot = toPrefixWithSlash(relativePathFromRoot);

        long size;
        try {
            size =  Files.size(documentPathReal);
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Lesen des Dokuments %s: %s.",
                    StringUtils.quote(pathFromRoot),
                    e.getMessage());
        }

        var document = new StorageDocument(relativePathFromRoot, documentPathReal.getFileName().toString(), size, StorageItemMetadata.empty());
        return Optional.of(document);
    }

    @Nonnull
    @Override
    public InputStream retrieveDocumentContent(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var documentPathReal = getSecurePath(config.getRealRootPath(), pathFromRoot);
        try {
            return Files.newInputStream(documentPathReal);
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Lesen des Dokuments %s: %s.",
                    StringUtils.quote(pathFromRoot),
                    e.getMessage());
        }
    }

    @Override
    public boolean documentExists(@Nonnull Config config, @Nonnull String path) {
        var documentPathReal = getSecurePath(config.getRealRootPath(), path);
        return Files.exists(documentPathReal) && Files.isRegularFile(documentPathReal);
    }

    @Nonnull
    @Override
    public StorageDocument moveDocument(@Nonnull Config config,
                                        @Nonnull String sourcePathFromRoot,
                                        @Nonnull String targetPathFromRoot) throws StorageException {
        var sourcePathReal = getSecurePath(config.getRealRootPath(), sourcePathFromRoot);
        var targetPathReal = getSecurePath(config.getRealRootPath(), targetPathFromRoot);

        if (!Files.exists(sourcePathReal) || !Files.isRegularFile(sourcePathReal)) {
            throw new StorageException("Das Quelldokument %s konnte nicht gefunden werden.", StringUtils.quote(sourcePathFromRoot));
        }

        var targetParentDirectoryReal = targetPathReal.getParent();
        if (targetParentDirectoryReal == null || !Files.exists(targetParentDirectoryReal) || !Files.isDirectory(targetParentDirectoryReal)) {
            throw new StorageException("Das Zielverzeichnis für das Dokument %s existiert nicht.", StringUtils.quote(targetPathFromRoot));
        }

        var sanitizedTargetFilename = sanitizeFilename(targetPathReal.getFileName().toString());
        var sanitizedTargetPathReal = targetParentDirectoryReal.resolve(sanitizedTargetFilename);

        try {
            Files.move(sourcePathReal, sanitizedTargetPathReal, StandardCopyOption.REPLACE_EXISTING);
            var relativeTargetPathFromRoot = config.getRealRootPath().relativize(sanitizedTargetPathReal).toString();
            relativeTargetPathFromRoot = toPrefixWithSlash(relativeTargetPathFromRoot);
            var sizeInBytes = Files.size(sanitizedTargetPathReal);
            return new StorageDocument(relativeTargetPathFromRoot, sanitizedTargetFilename, sizeInBytes, StorageItemMetadata.empty());
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Verschieben des Dokuments von %s nach %s: %s.",
                    StringUtils.quote(sourcePathFromRoot),
                    StringUtils.quote(targetPathFromRoot),
                    e.getMessage());
        }
    }

    @Nonnull
    @Override
    public StorageDocument copyDocument(@Nonnull Config config,
                                        @Nonnull String sourcePathFromRoot,
                                        @Nonnull String targetPathFromRoot) throws StorageException {
        var sourcePathReal = getSecurePath(config.getRealRootPath(), sourcePathFromRoot);
        var targetPathReal = getSecurePath(config.getRealRootPath(), targetPathFromRoot);

        if (!Files.exists(sourcePathReal) || !Files.isRegularFile(sourcePathReal)) {
            throw new StorageException("Das Quelldokument %s konnte nicht gefunden werden.", StringUtils.quote(sourcePathFromRoot));
        }

        var targetParentDirectoryReal = targetPathReal.getParent();
        if (targetParentDirectoryReal == null || !Files.exists(targetParentDirectoryReal) || !Files.isDirectory(targetParentDirectoryReal)) {
            throw new StorageException("Das Zielverzeichnis für das Dokument %s existiert nicht.", StringUtils.quote(targetPathFromRoot));
        }

        var sanitizedTargetFilename = sanitizeFilename(targetPathReal.getFileName().toString());
        var sanitizedTargetPathReal = targetParentDirectoryReal.resolve(sanitizedTargetFilename);

        try {
            Files.copy(sourcePathReal, sanitizedTargetPathReal, StandardCopyOption.REPLACE_EXISTING);
            var relativeTargetPathFromRoot = config.getRealRootPath().relativize(sanitizedTargetPathReal).toString();
            relativeTargetPathFromRoot = toPrefixWithSlash(relativeTargetPathFromRoot);
            var sizeInBytes = Files.size(sanitizedTargetPathReal);
            return new StorageDocument(relativeTargetPathFromRoot, sanitizedTargetFilename, sizeInBytes, StorageItemMetadata.empty());
        } catch (IOException e) {
            throw new StorageException(e,
                    "Fehler beim Kopieren des Dokuments von %s nach %s: %s.",
                    StringUtils.quote(sourcePathFromRoot),
                    StringUtils.quote(targetPathFromRoot),
                    e.getMessage());
        }
    }

    @Override
    public void deleteDocument(@Nonnull Config config, @Nonnull String path) throws StorageException {
        var documentPathReal = getSecurePath(config.getRealRootPath(), path);
        try {
            Files.delete(documentPathReal);
        } catch (IOException e) {
            if (e instanceof java.nio.file.NoSuchFileException) {
                // If the file does not exist, we can consider it as already deleted, so we ignore this exception
                return;
            }

            throw new StorageException(e,
                    "Fehler beim Löschen des Dokuments %s: %s.",
                    StringUtils.quote(path),
                    e.getMessage());
        }
    }

    /**
     * Get a pathFromRoot resolved against a base directory with basic sanitization to prevent pathFromRoot traversal attacks. Given the base directory /home/user/storage and the
     * input pathFromRoot /test.txt will return /home/user/storage/test.txt. Given the base directory /home/user/storage and the input pathFromRoot / will return
     * /home/user/storage/. Given the base directory /home/user/storage and the input pathFromRoot ./subdir/file.txt will return /home/user/storage/subdir/file.txt. Given the base
     * directory /home/user/storage and the input pathFromRoot /../etc/passwd will throw a SecurityException.
     *
     * @param baseDirectory The base directory to resolve against.
     * @param path          The input pathFromRoot to sanitize and resolve.
     * @return The securely resolved pathFromRoot.
     */
    @Nonnull
    protected static Path getSecurePath(@Nonnull Path baseDirectory, @Nonnull String path) {
        // Normalize the base directory to ensure consistent comparison
        Path normalizedBase = baseDirectory.toAbsolutePath().normalize();

        // Resolve the input pathFromRoot against the base directory and normalize
        Path resolved;
        if (path.startsWith(".")) {
            resolved = normalizedBase.resolve(path).normalize();
        } else {
            resolved = normalizedBase.resolve("." + path).normalize();
        }

        // Ensure the resolved pathFromRoot is within the base directory
        if (!resolved.startsWith(normalizedBase)) {
            throw new SecurityException("Path traversal attempt detected: " + path);
        }

        return resolved;
    }

    /**
     * Sanitize a filename by removing potentially dangerous characters and patterns. This method ensures the filename does not contain pathFromRoot segments, special characters,
     * or hidden file indicators.
     *
     * @param filename The input filename to sanitize.
     * @return The sanitized filename.
     */
    @Nonnull
    protected static String sanitizeFilename(@Nullable String filename) {
        if (filename == null || filename.isEmpty()) {
            return "default_filename_" + System.currentTimeMillis();
        }

        // 1. Replace all non-alphanumeric/dot/hyphen characters with an underscore
        // This removes spaces, slashes, and special characters like : * ? " < > |
        var sanitized = filename
                .replaceAll("[^a-zA-Z0-9( .)_-]", "_");

        // 2. Prevent hidden files (starting with a dot) or empty names
        if (sanitized.startsWith(".") || sanitized.isEmpty()) {
            sanitized = "file_" + sanitized;
        }

        return sanitized;
    }

    @LayoutElementPOJOBinding(id = "config", type = ElementType.ConfigLayout)
    public static class Config {
        @InputElementPOJOBinding(id = "root", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Stammverzeichnis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Verzeichnis auf dem Server, in dem die Dokumente gespeichert werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String root;

        public Path getRealRootPath() {
            return Path.of(root).toAbsolutePath().normalize();
        }
    }
}

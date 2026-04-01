package de.aivot.GoverBackend.plugins.core.v1.storage;

import de.aivot.GoverBackend.TestData;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalDiskStorageProviderDefinitionTest {

    private LocalDiskStorageProviderDefinitionV1.Config config;
    private LocalDiskStorageProviderDefinitionV1 provider;

    private static final String testRoot = ".storage-test/";
    private static final String documentsFolder = "/documents/";

    @BeforeEach
    void setUp() throws IOException {
        config = new LocalDiskStorageProviderDefinitionV1.Config();
        config.root = testRoot;

        Files.createDirectories(Path.of(config.root));
        Files.createDirectories(Path.of(config.root, documentsFolder));

        provider = createProvider(List.of(config.getRealRootPath().toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files
                .walk(Path.of(config.root))
                .map(Path::toFile)
                .sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                .forEach(file -> {
                    if (!file.delete()) {
                        System.err.println("Failed to delete " + file);
                    }
                });
    }

    @Test
    void rootFolder() throws StorageException {
        var root = provider.rootFolder(config, true);
        assertNotNull(root);

        root = provider.rootFolder(config, false);
        assertNotNull(root);
    }

    @Test
    void createFolder() throws StorageException {
        var target = provider.createFolder(config, "/new-folder");

        // Test returned folder object
        assertNotNull(target);
        assertEquals("new-folder", target.getName());
        assertEquals("/new-folder/", target.getPathFromRoot());

        // Test folder exists in filesystem
        assertTrue(Files.isDirectory(Path.of(config.root, "new-folder")));
    }

    @Test
    void retrieveFolder() throws StorageException {
        var target = provider.retrieveFolder(config, "/" + documentsFolder, true)
                .orElse(null);
        assertNotNull(target);
        assertEquals("documents", target.getName());
        assertEquals("/" + documentsFolder, target.getPathFromRoot());

        target = provider.retrieveFolder(config, "/" + documentsFolder, false)
                .orElse(null);
        assertNotNull(target);
        assertEquals("documents", target.getName());
        assertEquals("/" + documentsFolder, target.getPathFromRoot());
    }

    @Test
    void folderExists() {
        var exists = provider.folderExists(config, "/" + documentsFolder);
        assertTrue(exists);

        exists = provider.folderExists(config, "/non-existing-folder");
        assertFalse(exists);
    }

    @Test
    void deleteFolder() throws StorageException {
        var folderPath = "/folder-to-delete";

        // Create folder to delete
        provider.createFolder(config, folderPath);
        assertTrue(provider.folderExists(config, folderPath));

        // Delete folder
        provider.deleteFolder(config, folderPath);
        assertFalse(provider.folderExists(config, folderPath));
    }

    @Test
    void moveFolder() throws StorageException {
        var sourceFolderPath = "/documents/source-folder/";
        var targetFolderPath = "/documents/moved-folder/";
        var sourceDocumentPath = "/documents/source-folder/file.txt";
        var sourceDocumentData = "Moved folder content." .getBytes();

        provider.createFolder(config, sourceFolderPath);
        provider.storeDocument(config, sourceDocumentPath, new ByteArrayInputStream(sourceDocumentData), new StorageItemMetadata());

        var movedFolder = provider.moveFolder(config, sourceFolderPath, targetFolderPath);

        assertNotNull(movedFolder);
        assertEquals("/documents/moved-folder/", movedFolder.getPathFromRoot());
        assertFalse(provider.folderExists(config, sourceFolderPath));
        assertTrue(provider.folderExists(config, targetFolderPath));
        assertTrue(provider.documentExists(config, "/documents/moved-folder/file.txt"));
    }

    @Test
    void copyFolder() throws StorageException {
        var sourceFolderPath = "/documents/source-copy-folder/";
        var targetFolderPath = "/documents/copied-folder/";
        var sourceDocumentPath = "/documents/source-copy-folder/file.txt";
        var sourceDocumentData = "Copied folder content." .getBytes();

        provider.createFolder(config, sourceFolderPath);
        provider.storeDocument(config, sourceDocumentPath, new ByteArrayInputStream(sourceDocumentData), new StorageItemMetadata());

        var copiedFolder = provider.copyFolder(config, sourceFolderPath, targetFolderPath);

        assertNotNull(copiedFolder);
        assertEquals("/documents/copied-folder/", copiedFolder.getPathFromRoot());
        assertTrue(provider.folderExists(config, sourceFolderPath));
        assertTrue(provider.folderExists(config, targetFolderPath));
        assertTrue(provider.documentExists(config, "/documents/source-copy-folder/file.txt"));
        assertTrue(provider.documentExists(config, "/documents/copied-folder/file.txt"));
    }

    @Test
    void storeDocument() throws StorageException {
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document." .getBytes();
        var document = provider.storeDocument(config, documentPath, new ByteArrayInputStream(documentData), new StorageItemMetadata());
        assertNotNull(document);
        assertEquals("test-document.txt", document.getName());
        assertEquals("/documents/test-document.txt", document.getPathFromRoot());
        assertEquals("txt", document.getExtension());
    }

    @Test
    void retrieveDocument() throws StorageException {
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document." .getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, new ByteArrayInputStream(documentData), new StorageItemMetadata());

        // Retrieve document
        var retrievedDocumentOpt = provider.retrieveDocument(config, documentPath);
        assertTrue(retrievedDocumentOpt.isPresent());
        var retrievedDocument = retrievedDocumentOpt.get();
        assertEquals("test-document.txt", retrievedDocument.getName());
        assertEquals("/documents/test-document.txt", retrievedDocument.getPathFromRoot());
        assertEquals("txt", retrievedDocument.getExtension());
    }

    @Test
    void retrieveDocumentContent() throws StorageException {
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document." .getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, new ByteArrayInputStream(documentData), new StorageItemMetadata());

        // Retrieve document content
        try (var inputStream = provider.retrieveDocumentContent(config, documentPath)) {
            byte[] retrievedData = inputStream.readAllBytes();
            assertArrayEquals(documentData, retrievedData);
        } catch (IOException e) {
            fail("IOException occurred while retrieving document content: " + e.getMessage());
        }
    }

    @Test
    void documentExists() throws StorageException {
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document." .getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, new ByteArrayInputStream(documentData), new StorageItemMetadata());

        // Check existence
        var exists = provider.documentExists(config, documentPath);
        assertTrue(exists);

        exists = provider.documentExists(config, "/documents/non-existing-document.txt");
        assertFalse(exists);
    }

    @Test
    void deleteDocument() throws StorageException {
        var documentPath = "/documents/document-to-delete.txt";
        var documentData = "This document will be deleted." .getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, new ByteArrayInputStream(documentData), new StorageItemMetadata());
        assertTrue(provider.documentExists(config, documentPath));

        // Delete document
        provider.deleteDocument(config, documentPath);
        assertFalse(provider.documentExists(config, documentPath));
    }

    @Test
    void moveDocument() throws StorageException {
        var sourcePath = "/documents/source.txt";
        var targetPath = "/documents/moved.txt";
        var documentData = "This document will be moved." .getBytes();

        provider.storeDocument(config, sourcePath, new ByteArrayInputStream(documentData), new StorageItemMetadata());
        assertTrue(provider.documentExists(config, sourcePath));

        var movedDocument = provider.moveDocument(config, sourcePath, targetPath);

        assertNotNull(movedDocument);
        assertEquals(targetPath, movedDocument.getPathFromRoot());
        assertTrue(provider.documentExists(config, targetPath));
        assertFalse(provider.documentExists(config, sourcePath));
    }

    @Test
    void copyDocument() throws StorageException {
        var sourcePath = "/documents/source-copy.txt";
        var targetPath = "/documents/copied.txt";
        var documentData = "This document will be copied." .getBytes();

        provider.storeDocument(config, sourcePath, new ByteArrayInputStream(documentData), new StorageItemMetadata());
        assertTrue(provider.documentExists(config, sourcePath));

        var copiedDocument = provider.copyDocument(config, sourcePath, targetPath);

        assertNotNull(copiedDocument);
        assertEquals(targetPath, copiedDocument.getPathFromRoot());
        assertTrue(provider.documentExists(config, sourcePath));
        assertTrue(provider.documentExists(config, targetPath));
    }

    @Test
    void validateConfigurationRejectsWhenAllowedLocalRootsEmpty() {
        var exception = assertThrows(
                ResponseException.class,
                () -> createProvider(List.of()).validateConfiguration(providerEntity(0), config)
        );

        assertEquals(
                "Lokale Speicheranbieter können nicht erstellt werden, da keine erlaubten lokalen Stammverzeichnisse über storage.local.v1.allowed-local-roots konfiguriert sind.",
                exception.getMessage()
        );
    }

    @Test
    void validateConfigurationAllowsRootUnderConfiguredLocalRoot() {
        var allowedRoot = config.getRealRootPath().getParent();
        assertNotNull(allowedRoot);

        assertDoesNotThrow(() ->
                createProvider(List.of(allowedRoot.toString())).validateConfiguration(providerEntity(0), config)
        );
    }

    @Test
    void validateConfigurationRejectsRootOutsideConfiguredLocalRoots() {
        var allowedRoot = config.getRealRootPath()
                .getParent()
                .resolve("outside-allowed-root");

        var exception = assertThrows(
                ResponseException.class,
                () -> createProvider(List.of(allowedRoot.toString())).validateConfiguration(providerEntity(0), config)
        );

        assertEquals(
                "Das Stammverzeichnis %s ist nicht zulässig. Es muss unter einem der konfigurierten erlaubten lokalen Stammverzeichnisse liegen."
                        .formatted(StringUtils.quote(config.getRealRootPath().toString())),
                exception.getMessage()
        );
    }

    @Test
    void validateConfigurationRejectsOverlappingParentRoot() {
        var targetRoot = config.getRealRootPath();
        var parentRoot = targetRoot.getParent();
        assertNotNull(parentRoot);

        var exception = assertThrows(
                ResponseException.class,
                () -> createProvider(
                        List.of(parentRoot.toString()),
                        List.of(existingProvider(7, "Parent Provider", parentRoot.toString()))
                ).validateConfiguration(providerEntity(70), config)
        );

        assertEquals(
                "Das Stammverzeichnis %s überschneidet sich mit dem Stammverzeichnis %s des lokalen Speicheranbieters %s mit der ID %s. Lokale Speicheranbieter dürfen keine überlappenden Stammverzeichnisse verwenden."
                        .formatted(
                                StringUtils.quote(targetRoot.toString()),
                                StringUtils.quote(parentRoot.toString()),
                                StringUtils.quote("Parent Provider"),
                                StringUtils.quote("7")
                        ),
                exception.getMessage()
        );
    }

    @Test
    void validateConfigurationRejectsOverlappingSubpathRoot() {
        var targetRoot = config.getRealRootPath();
        var subpathRoot = targetRoot.resolve("subfolder");
        var allowedRoot = targetRoot.getParent();
        assertNotNull(allowedRoot);

        var exception = assertThrows(
                ResponseException.class,
                () -> createProvider(
                        List.of(allowedRoot.toString()),
                        List.of(existingProvider(8, "Subpath Provider", subpathRoot.toString()))
                ).validateConfiguration(providerEntity(80), config)
        );

        assertEquals(
                "Das Stammverzeichnis %s überschneidet sich mit dem Stammverzeichnis %s des lokalen Speicheranbieters %s mit der ID %s. Lokale Speicheranbieter dürfen keine überlappenden Stammverzeichnisse verwenden."
                        .formatted(
                                StringUtils.quote(targetRoot.toString()),
                                StringUtils.quote(subpathRoot.toString()),
                                StringUtils.quote("Subpath Provider"),
                                StringUtils.quote("8")
                        ),
                exception.getMessage()
        );
    }

    @Test
    void validateConfigurationAllowsExistingRootForSameProviderId() {
        var targetRoot = config.getRealRootPath();
        var allowedRoot = targetRoot.getParent();
        assertNotNull(allowedRoot);

        assertDoesNotThrow(() ->
                createProvider(
                        List.of(allowedRoot.toString()),
                        List.of(existingProvider(9, "Current Provider", targetRoot.toString()))
                ).validateConfiguration(providerEntity(9), config)
        );
    }

    @Test
    void getSecurePath() {
        var base = Path.of("/tmp/storage");

        // Test valid paths
        Path result = LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "folder/file.txt");
        assertTrue(result.startsWith(base));

        // Test absolute pathFromRoot
        result = LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "/file.txt");
        assertTrue(result.startsWith(base));

        // Test pathFromRoot traversal attempts
        assertThrows(SecurityException.class, () ->
                LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "../etc/passwd")
        );

        // Test edge cases
        result = LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "/");
        assertEquals(base.normalize(), result);

        // Test relative paths
        result = LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "./subdir/file.txt");
        assertTrue(result.startsWith(base));

        // Test complex traversal
        assertThrows(SecurityException.class, () ->
                LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "../../outside.txt")
        );

        // Test empty pathFromRoot
        result = LocalDiskStorageProviderDefinitionV1.getSecurePath(base, "");
        assertEquals(base.normalize(), result);
    }

    @Test
    void sanitizeFilename() {
        // Test normal filename
        String result = LocalDiskStorageProviderDefinitionV1.sanitizeFilename("document.txt");
        assertEquals("document.txt", result);

        // Test filename with spaces
        result = LocalDiskStorageProviderDefinitionV1.sanitizeFilename("my document.txt");
        assertEquals("my document.txt", result);

        // Test filename with special characters
        result = LocalDiskStorageProviderDefinitionV1.sanitizeFilename("docu<>ment?.txt");
        assertEquals("docu__ment_.txt", result);

        // Test filename with only invalid characters
        result = LocalDiskStorageProviderDefinitionV1.sanitizeFilename("<>:\"/\\|?*");
        assertEquals("_________", result);

        // Test empty filename
        result = LocalDiskStorageProviderDefinitionV1.sanitizeFilename("");
        assertNotEquals("", result);

        // Test filename with unicode characters
        result = LocalDiskStorageProviderDefinitionV1.sanitizeFilename("döcümént.txt");
        assertEquals("d_c_m_nt.txt", result);
    }

    private LocalDiskStorageProviderDefinitionV1 createProvider(List<String> allowedLocalRoots) {
        return createProvider(allowedLocalRoots, List.of());
    }

    private LocalDiskStorageProviderDefinitionV1 createProvider(List<String> allowedLocalRoots,
                                                                List<StorageProviderEntity> existingProviders) {
        var storageProviderRepository = mock(StorageProviderRepository.class);
        when(storageProviderRepository.findAllByStorageProviderDefinitionKey(anyString()))
                .thenReturn(existingProviders);

        return new LocalDiskStorageProviderDefinitionV1(
                new LocalDiskStorageProviderDefinitionPropertiesV1()
                        .setAllowedLocalRoots(allowedLocalRoots),
                storageProviderRepository
        );
    }

    private StorageProviderEntity existingProvider(int id, String name, String root) {
        return new StorageProviderEntity()
                .setId(id)
                .setName(name)
                .setConfiguration(TestData.authored("root", root));
    }

    private StorageProviderEntity providerEntity(int id) {
        return new StorageProviderEntity()
                .setId(id);
    }
}

package de.aivot.GoverBackend.plugins.core.v1.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalDiskStorageProviderDefinitionTest {

    private LocalDiskStorageProviderDefinition.Config config;

    private static final String testRoot = ".storage-test/";
    private static final String documentsFolder = "documents/";

    @BeforeEach
    void setUp() throws IOException {
        config = new LocalDiskStorageProviderDefinition.Config();
        config.root = testRoot;

        Files.createDirectories(Path.of(config.root));
        Files.createDirectories(Path.of(config.root, documentsFolder));
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
    void rootFolder() {
        var root = new LocalDiskStorageProviderDefinition()
                .rootFolder(config, true);
        assertNotNull(root);

        root = new LocalDiskStorageProviderDefinition()
                .rootFolder(config, false);
        assertNotNull(root);
    }

    @Test
    void createFolder() {
        var target = new LocalDiskStorageProviderDefinition()
                .createFolder(config, "/new-folder");

        // Test returned folder object
        assertNotNull(target);
        assertEquals("new-folder", target.getName());
        assertEquals("/new-folder", target.getPathFromRoot());

        // Test folder exists in filesystem
        assertTrue(Files.isDirectory(Path.of(config.root, "new-folder")));
    }

    @Test
    void retrieveFolder() {
        var target = new LocalDiskStorageProviderDefinition()
                .retrieveFolder(config, "/" + documentsFolder, true)
                .orElse(null);
        assertNotNull(target);
        assertEquals("documents", target.getName());
        assertEquals("/" + documentsFolder, target.getPathFromRoot());

        target = new LocalDiskStorageProviderDefinition()
                .retrieveFolder(config, "/" + documentsFolder, false)
                .orElse(null);
        assertNotNull(target);
        assertEquals("documents", target.getName());
        assertEquals("/" + documentsFolder, target.getPathFromRoot());
    }

    @Test
    void folderExists() {
        var exists = new LocalDiskStorageProviderDefinition()
                .folderExists(config, "/" + documentsFolder);
        assertTrue(exists);

        exists = new LocalDiskStorageProviderDefinition()
                .folderExists(config, "/non-existing-folder");
        assertFalse(exists);
    }

    @Test
    void deleteFolder() {
        var provider = new LocalDiskStorageProviderDefinition();
        var folderPath = "/folder-to-delete";

        // Create folder to delete
        provider.createFolder(config, folderPath);
        assertTrue(provider.folderExists(config, folderPath));

        // Delete folder
        provider.deleteFolder(config, folderPath);
        assertFalse(provider.folderExists(config, folderPath));
    }

    @Test
    void storeDocument() {
        var provider = new LocalDiskStorageProviderDefinition();
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document.".getBytes();
        var document = provider.storeDocument(config, documentPath, documentData);
        assertNotNull(document);
        assertEquals("test-document.txt", document.filename());
        assertEquals("/documents/test-document.txt", document.pathFromRoot());
        assertEquals("txt", document.extension());
    }

    @Test
    void retrieveDocument() {
        var provider = new LocalDiskStorageProviderDefinition();
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document.".getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, documentData);

        // Retrieve document
        var retrievedDocumentOpt = provider.retrieveDocument(config, documentPath);
        assertTrue(retrievedDocumentOpt.isPresent());
        var retrievedDocument = retrievedDocumentOpt.get();
        assertEquals("test-document.txt", retrievedDocument.filename());
        assertEquals("/documents/test-document.txt", retrievedDocument.pathFromRoot());
        assertEquals("txt", retrievedDocument.extension());
    }

     @Test
    void retrieveDocumentContent() {
        var provider = new LocalDiskStorageProviderDefinition();
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document.".getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, documentData);

        // Retrieve document content
        try (var inputStream = provider.retrieveDocumentContent(config, documentPath)) {
            byte[] retrievedData = inputStream.readAllBytes();
            assertArrayEquals(documentData, retrievedData);
        } catch (IOException e) {
            fail("IOException occurred while retrieving document content: " + e.getMessage());
        }
     }

    @Test
    void documentExists() {
        var provider = new LocalDiskStorageProviderDefinition();
        var documentPath = "/documents/test-document.txt";
        var documentData = "This is a test document.".getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, documentData);

        // Check existence
        var exists = provider.documentExists(config, documentPath);
        assertTrue(exists);

        exists = provider.documentExists(config, "/documents/non-existing-document.txt");
        assertFalse(exists);
    }

    @Test
    void deleteDocument() {
        var provider = new LocalDiskStorageProviderDefinition();
        var documentPath = "/documents/document-to-delete.txt";
        var documentData = "This document will be deleted.".getBytes();

        // Store document first
        provider.storeDocument(config, documentPath, documentData);
        assertTrue(provider.documentExists(config, documentPath));

        // Delete document
        provider.deleteDocument(config, documentPath);
        assertFalse(provider.documentExists(config, documentPath));
    }

    @Test
    void getSecurePath() {
        var base = Path.of("/tmp/storage");

        // Test valid paths
        Path result = LocalDiskStorageProviderDefinition.getSecurePath(base, "folder/file.txt");
        assertTrue(result.startsWith(base));

        // Test absolute pathFromRoot
        result = LocalDiskStorageProviderDefinition.getSecurePath(base, "/file.txt");
        assertTrue(result.startsWith(base));

        // Test pathFromRoot traversal attempts
        assertThrows(SecurityException.class, () ->
                LocalDiskStorageProviderDefinition.getSecurePath(base, "../etc/passwd")
        );

        // Test edge cases
        result = LocalDiskStorageProviderDefinition.getSecurePath(base, "/");
        assertEquals(base.normalize(), result);

        // Test relative paths
        result = LocalDiskStorageProviderDefinition.getSecurePath(base, "./subdir/file.txt");
        assertTrue(result.startsWith(base));

        // Test complex traversal
        assertThrows(SecurityException.class, () ->
                LocalDiskStorageProviderDefinition.getSecurePath(base, "../../outside.txt")
        );

        // Test empty pathFromRoot
        result = LocalDiskStorageProviderDefinition.getSecurePath(base, "");
        assertEquals(base.normalize(), result);
    }

    @Test
    void sanitizeFilename() {
        // Test normal filename
        String result = LocalDiskStorageProviderDefinition.sanitizeFilename("document.txt");
        assertEquals("document.txt", result);

        // Test filename with spaces
        result = LocalDiskStorageProviderDefinition.sanitizeFilename("my document.txt");
        assertEquals("my_document.txt", result);

        // Test filename with special characters
        result = LocalDiskStorageProviderDefinition.sanitizeFilename("docu<>ment?.txt");
        assertEquals("docu__ment_.txt", result);

        // Test filename with only invalid characters
        result = LocalDiskStorageProviderDefinition.sanitizeFilename("<>:\"/\\|?*");
        assertEquals("_________", result);

        // Test empty filename
        result = LocalDiskStorageProviderDefinition.sanitizeFilename("");
        assertNotEquals("", result);

        // Test filename with unicode characters
        result = LocalDiskStorageProviderDefinition.sanitizeFilename("döcümént.txt");
        assertEquals("d_c_m_nt.txt", result);
    }
}
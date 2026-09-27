package de.aivot.prosuna.backend.storage.utils;

import de.aivot.prosuna.backend.storage.exceptions.StorageException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoragePathUtilsTest {
    @Test
    void normalizeDocumentPathDecodesPercentEscapesWithoutTreatingPlusAsSpace() throws StorageException {
        assertEquals(
                "/folder/file name+.txt",
                StoragePathUtils.normalizeDocumentPath("folder/file%20name+.txt")
        );
        assertEquals(
                "/folder/file+.txt",
                StoragePathUtils.normalizeDocumentPath("/folder/file%2b.txt")
        );
    }

    @Test
    void normalizeFolderPathNormalizesDecodedSeparators() throws StorageException {
        assertEquals(
                "/folder/sub/",
                StoragePathUtils.normalizeFolderPath("\\folder%2fsub")
        );
    }

    @Test
    void normalizePathRejectsLiteralTraversalSegments() {
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeFolderPath("/folder/./"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder/../file.txt"));
    }

    @Test
    void normalizePathRejectsPercentEncodedTraversalSegments() {
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeFolderPath("/%2e/"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeFolderPath("/%2e%2e/"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeFolderPath("/%2E%2E/"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder%2f..%2ffile.txt"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder%5c..%5cfile.txt"));
    }

    @Test
    void normalizePathRejectsInvalidPercentEscapes() {
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder/%"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder/%2"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder/%zz/file.txt"));
        assertThrows(StorageException.class, () -> StoragePathUtils.normalizeDocumentPath("/folder/%c3%28/file.txt"));
    }
}

package de.aivot.gover.backend.plugins.core.v1.storage;

import de.aivot.gover.backend.plugins.core.v1.storage.S3StorageProviderDefinitionV1;
import de.aivot.gover.backend.secrets.repositories.SecretRepository;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.storage.exceptions.StorageException;
import de.aivot.gover.backend.storage.models.StorageDocument;
import de.aivot.gover.backend.storage.models.StorageFolder;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.storage.services.KnownExtensionsService;
import io.minio.CopyObjectArgs;
import io.minio.Directive;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageProviderDefinitionV1Test {

    @Test
    void createFolderUploadsZeroByteMarkerObject() throws Exception {
        var client = mock(MinioClient.class);
        var provider = new TestS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        var folder = provider.createFolder(config, "/new-folder");

        var putObjectArgsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(putObjectArgsCaptor.capture());
        var putObjectArgs = putObjectArgsCaptor.getValue();

        assertEquals("new-folder/", putObjectArgs.object());
        assertEquals(0L, putObjectArgs.objectSize());
        assertEquals("/new-folder/", folder.getPathFromRoot());
    }

    @Test
    void retrieveFolderTreatsSlashObjectAsSubfolder() throws Exception {
        var client = mock(MinioClient.class);
        var item = mock(Item.class);
        when(item.objectName()).thenReturn("empty/");

        Iterable<Result<Item>> objects = List.of(new Result<>(item));
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(objects);

        var provider = new ClientOnlyS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        var folder = provider.retrieveFolder(config, "/", false).orElseThrow();

        assertTrue(folder.getDocuments().isEmpty());
        assertEquals(1, folder.getSubfolders().size());
        assertEquals("/empty/", folder.getSubfolders().get(0).getPathFromRoot());
        verify(client).listObjects(any(ListObjectsArgs.class));
    }

    @Test
    void retrieveFolderReturnsFolderForItsSlashMarker() throws Exception {
        var client = mock(MinioClient.class);
        var item = mock(Item.class);
        when(item.objectName()).thenReturn("empty/");

        Iterable<Result<Item>> objects = List.of(new Result<>(item));
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(objects);

        var provider = new ClientOnlyS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        var folder = provider.retrieveFolder(config, "/empty/", false).orElseThrow();

        assertEquals("/empty/", folder.getPathFromRoot());
        assertTrue(folder.getDocuments().isEmpty());
        assertTrue(folder.getSubfolders().isEmpty());
    }

    @Test
    void copyFolderDoesNotDeleteTargetWhenSourceFolderIsMissing() throws Exception {
        var client = mock(MinioClient.class);
        Iterable<Result<Item>> noObjects = List.of();
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(noObjects);

        var provider = new TestS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        assertThrows(StorageException.class, () -> provider.copyFolder(config, "/missing/", "/target/"));

        assertTrue(provider.deletedFolders.isEmpty());
        verify(client, never()).copyObject(any());
    }

    @Test
    void moveFolderDoesNotDeleteTargetWhenSourceFolderIsMissing() throws Exception {
        var client = mock(MinioClient.class);
        Iterable<Result<Item>> noObjects = List.of();
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(noObjects);

        var provider = new TestS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        assertThrows(StorageException.class, () -> provider.moveFolder(config, "/missing/", "/target/"));

        assertTrue(provider.deletedFolders.isEmpty());
        verify(client, never()).copyObject(any());
    }

    @Test
    void copyFolderDeletesTargetAfterSourceObjectWasFound() throws Exception {
        var client = mock(MinioClient.class);
        var item = mock(Item.class);
        when(item.objectName()).thenReturn("source/file.txt");

        Iterable<Result<Item>> sourceObjects = List.of(new Result<>(item));
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(sourceObjects);

        var provider = new TestS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        var copiedFolder = provider.copyFolder(config, "/source/", "/target/");

        assertEquals("/target/", copiedFolder.getPathFromRoot());
        assertEquals(List.of("/target/"), provider.deletedFolders);
        verify(client).copyObject(any());
    }

    @Test
    void updateDocumentMetadataReplacesMetadataAndPreservesContentType() throws Exception {
        var client = mock(MinioClient.class);
        var statObjectResponse = mock(StatObjectResponse.class);
        when(statObjectResponse.contentType()).thenReturn("application/pdf");
        when(statObjectResponse.size()).thenReturn(123L);
        when(statObjectResponse.userMetadata()).thenReturn(Map.of("color", "blue"));
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);

        var provider = new TestS3StorageProviderDefinitionV1(client);
        var config = createConfig();
        var metadata = new StorageItemMetadata();
        metadata.put("color", "blue");

        StorageDocument updatedDocument = provider.updateDocumentMetadata(config, "/document.pdf", metadata);

        var copyObjectArgsCaptor = ArgumentCaptor.forClass(CopyObjectArgs.class);
        verify(client).copyObject(copyObjectArgsCaptor.capture());
        var copyObjectArgs = copyObjectArgsCaptor.getValue();

        assertEquals(Directive.REPLACE, copyObjectArgs.metadataDirective());
        assertTrue(copyObjectArgs.userMetadata().values().contains("blue"));
        assertTrue(copyObjectArgs.headers().get("Content-Type").contains("application/pdf"));
        assertEquals("/document.pdf", updatedDocument.getPathFromRoot());
        assertEquals("blue", updatedDocument.getMetadata().get("x-amz-meta-color"));
    }

    @Test
    void retrieveDocumentUsesLastModifiedAsTimestamps() throws Exception {
        var client = mock(MinioClient.class);
        var statObjectResponse = mock(StatObjectResponse.class);
        var lastModified = ZonedDateTime.parse("2026-01-02T03:04:05Z");
        when(statObjectResponse.size()).thenReturn(123L);
        when(statObjectResponse.userMetadata()).thenReturn(Map.of());
        when(statObjectResponse.lastModified()).thenReturn(lastModified);
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);

        var provider = new TestS3StorageProviderDefinitionV1(client);
        var config = createConfig();

        var document = provider.retrieveDocument(config, "/document.pdf").orElseThrow();

        assertEquals(lastModified.toInstant(), document.getCreated());
        assertEquals(lastModified.toInstant(), document.getUpdated());
    }

    private static S3StorageProviderDefinitionV1.Config createConfig() {
        var config = new S3StorageProviderDefinitionV1.Config();
        config.bucket = "bucket";
        return config;
    }

    private static final class TestS3StorageProviderDefinitionV1 extends S3StorageProviderDefinitionV1 {
        private final MinioClient client;
        private final List<String> deletedFolders = new LinkedList<>();

        private TestS3StorageProviderDefinitionV1(MinioClient client) {
            super(mock(SecretRepository.class), mock(SecretService.class), mock(KnownExtensionsService.class), mock(StorageProviderRepository.class));
            this.client = client;
        }

        @Override
        MinioClient getClient(Config config) {
            return client;
        }

        @Override
        public void deleteFolder(Config config, String path) {
            deletedFolders.add(path);
        }

        @Override
        public Optional<StorageFolder> retrieveFolder(Config config, String pathFromRoot, boolean recursive) {
            return Optional.of(new StorageFolder(
                    pathFromRoot,
                    "target",
                    new LinkedList<>(),
                    new LinkedList<>(),
                    recursive
            ));
        }
    }

    private static final class ClientOnlyS3StorageProviderDefinitionV1 extends S3StorageProviderDefinitionV1 {
        private final MinioClient client;

        private ClientOnlyS3StorageProviderDefinitionV1(MinioClient client) {
            super(mock(SecretRepository.class), mock(SecretService.class), mock(KnownExtensionsService.class), mock(StorageProviderRepository.class));
            this.client = client;
        }

        @Override
        MinioClient getClient(Config config) {
            return client;
        }
    }
}

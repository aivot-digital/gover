package de.aivot.GoverBackend.plugins.core.v1.storage;

import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.storage.services.KnownExtensionsService;
import io.minio.CopyObjectArgs;
import io.minio.Directive;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
}

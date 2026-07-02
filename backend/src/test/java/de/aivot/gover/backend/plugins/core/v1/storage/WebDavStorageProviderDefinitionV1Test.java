package de.aivot.gover.backend.plugins.core.v1.storage;

import de.aivot.gover.backend.TestData;
import de.aivot.gover.backend.core.properties.HttpServiceProperties;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.secrets.repositories.SecretRepository;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.exceptions.StorageException;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.storage.services.KnownExtensionsService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebDavStorageProviderDefinitionV1Test {

    @Test
    void toWebDavUriCombinesBaseUrlBasePathAndProviderPath() throws StorageException {
        var config = createConfig();
        config.baseUrl = "https://example.test/remote.php/dav/files/user";
        config.basePath = "/gover/documents";

        var documentUri = WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/cases/123/file name.pdf");
        assertEquals(
                "https://example.test/remote.php/dav/files/user/gover/documents/cases/123/file%20name.pdf",
                documentUri.toString()
        );

        var folderUri = WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/cases/123/");
        assertEquals(
                "https://example.test/remote.php/dav/files/user/gover/documents/cases/123/",
                folderUri.toString()
        );
    }

    @Test
    void toWebDavUriRejectsTraversalInBasePath() {
        var config = createConfig();
        config.basePath = "/gover/../documents";

        assertThrows(StorageException.class, () -> WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/file.txt"));
    }

    @Test
    void parseMultiStatusReadsCollectionsAndDocumentSizes() throws StorageException {
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:multistatus xmlns:d="DAV:">
                  <d:response>
                    <d:href>/dav/gover/</d:href>
                    <d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop></d:propstat>
                  </d:response>
                  <d:response>
                    <d:href>/dav/gover/file.txt</d:href>
                    <d:propstat><d:prop><d:getcontentlength>42</d:getcontentlength></d:prop></d:propstat>
                  </d:response>
                </d:multistatus>
                """;

        var resources = WebDavStorageProviderDefinitionV1.parseMultiStatus(xml.getBytes());

        assertEquals(2, resources.size());
        assertEquals("/dav/gover/", resources.get(0).href());
        assertTrue(resources.get(0).collection());
        assertEquals(42L, resources.get(1).sizeInBytes());
    }

    @Test
    void retrieveFolderUsesBasePathAsInvisibleRoot() throws StorageException {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents/";

        var client = new FakeWebDavClient();
        client.propfindResponses.put(
                "/dav/gover/documents/",
                Optional.of(List.of(
                        new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents", true, 0L),
                        new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/sub/", true, 0L),
                        new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/file.txt", false, 12L)
                ))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        var folder = provider.retrieveFolder(config, "/", false).orElseThrow();

        assertEquals("/", folder.getPathFromRoot());
        assertEquals("Root", folder.getName());
        assertEquals("/sub/", folder.getSubfolders().getFirst().getPathFromRoot());
        assertEquals("/file.txt", folder.getDocuments().getFirst().getPathFromRoot());
        assertEquals(12L, folder.getDocuments().getFirst().getSizeInBytes());
    }

    @Test
    void storeDocumentRequiresParentAndPutsUnderBasePath() throws StorageException {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents/";

        var client = new FakeWebDavClient();
        client.propfindResponses.put(
                "/dav/gover/documents/folder/",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/folder/", true, 0L)))
        );
        client.propfindResponses.put(
                "/dav/gover/documents/folder/file.txt",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/folder/file.txt", false, 5L)))
        );

        var knownExtensionsService = mock(KnownExtensionsService.class);
        when(knownExtensionsService.determineMimeType(anyString())).thenReturn(Optional.of("text/plain"));

        var provider = createProvider(client, knownExtensionsService);

        var document = provider.storeDocument(
                config,
                "/folder/file.txt",
                new ByteArrayInputStream("hello".getBytes()),
                StorageItemMetadata.empty()
        );

        assertEquals(URI.create("https://example.test/dav/gover/documents/folder/file.txt"), client.putUri);
        assertEquals("text/plain", client.putContentType);
        assertEquals("/folder/file.txt", document.getPathFromRoot());
        assertEquals(5L, document.getSizeInBytes());
    }

    @Test
    void validateConfigurationRejectsOverlappingRoots() {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents/";

        var existingProvider = new StorageProviderEntity()
                .setId(7)
                .setName("Existing")
                .setConfiguration(TestData.authored(
                        "base_url", "https://example.test/dav/",
                        "base_path", "/gover/"
                ));

        var storageProviderRepository = mock(StorageProviderRepository.class);
        when(storageProviderRepository.findAllByStorageProviderDefinitionKey(anyString()))
                .thenReturn(List.of(existingProvider));

        var provider = createProvider(new FakeWebDavClient(), mock(KnownExtensionsService.class), storageProviderRepository);

        assertThrows(ResponseException.class, () -> provider.validateConfiguration(new StorageProviderEntity().setId(8), config));
    }

    private static WebDavStorageProviderDefinitionV1.Config createConfig() {
        var config = new WebDavStorageProviderDefinitionV1.Config();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/";
        config.username = "user";
        config.passwordSecret = "00000000-0000-0000-0000-000000000000";
        return config;
    }

    private static WebDavStorageProviderDefinitionV1 createProvider(WebDavStorageProviderDefinitionV1.WebDavClient client,
                                                                    KnownExtensionsService knownExtensionsService) {
        return createProvider(client, knownExtensionsService, mock(StorageProviderRepository.class));
    }

    private static WebDavStorageProviderDefinitionV1 createProvider(WebDavStorageProviderDefinitionV1.WebDavClient client,
                                                                    KnownExtensionsService knownExtensionsService,
                                                                    StorageProviderRepository storageProviderRepository) {
        return new WebDavStorageProviderDefinitionV1(
                mock(SecretRepository.class),
                mock(SecretService.class),
                knownExtensionsService,
                storageProviderRepository,
                httpProperties()
        ) {
            @Override
            WebDavClient getClient(Config config) {
                return client;
            }
        };
    }

    private static HttpServiceProperties httpProperties() {
        var properties = new HttpServiceProperties();
        properties.setConnectionTimeoutSeconds(1);
        properties.setReadTimeoutSeconds(1);
        return properties;
    }

    private static final class FakeWebDavClient extends WebDavStorageProviderDefinitionV1.WebDavClient {
        private final HashMap<String, Optional<List<WebDavStorageProviderDefinitionV1.WebDavRemoteResource>>> propfindResponses = new HashMap<>();
        private final List<URI> deletedUris = new LinkedList<>();
        private URI putUri;
        private String putContentType;

        private FakeWebDavClient() {
            super("user", "password", httpProperties());
        }

        @Override
        Optional<List<WebDavStorageProviderDefinitionV1.WebDavRemoteResource>> propfind(URI uri, int depth) {
            return propfindResponses.getOrDefault(uri.getPath(), Optional.empty());
        }

        @Override
        void mkcol(URI uri) {
        }

        @Override
        void put(URI uri, InputStream data, String contentType) {
            putUri = uri;
            putContentType = contentType;
        }

        @Override
        InputStream get(URI uri) {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        void delete(URI uri) {
            deletedUris.add(uri);
        }

        @Override
        void copy(URI sourceUri, URI targetUri) {
        }

        @Override
        void move(URI sourceUri, URI targetUri) {
        }
    }
}

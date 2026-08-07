package de.aivot.prosuna.backend.plugins.core.v1.storage;

import de.aivot.prosuna.backend.TestData;
import de.aivot.prosuna.backend.core.properties.HttpServiceProperties;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.secrets.repositories.SecretRepository;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.exceptions.StorageException;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import de.aivot.prosuna.backend.storage.services.KnownExtensionsService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void toWebDavUriRejectsPercentEncodedTraversalInBasePath() {
        var config = createConfig();
        config.basePath = "/gover/%2e%2e/documents";

        assertThrows(StorageException.class, () -> WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/file.txt"));
    }

    @Test
    void toWebDavUriRejectsPercentEncodedTraversalInProviderPath() {
        var config = createConfig();

        assertThrows(StorageException.class, () -> WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/%2e%2e/file.txt"));
        assertThrows(StorageException.class, () -> WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/folder%2f..%2ffile.txt"));
        assertThrows(StorageException.class, () -> WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/folder%5c..%5cfile.txt"));
    }

    @Test
    void toWebDavUriRejectsInvalidPercentEscapes() {
        var config = createConfig();

        assertThrows(StorageException.class, () -> WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/folder/%zz/file.txt"));
    }

    @Test
    void toWebDavUriDoesNotTreatPlusAsSpace() throws StorageException {
        var config = createConfig();

        var uri = WebDavStorageProviderDefinitionV1.toWebDavUri(config, "/folder/file+name.txt");

        assertEquals("https://example.test/dav/folder/file+name.txt", uri.toString());
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
                    <d:propstat><d:prop>
                      <d:getcontentlength>42</d:getcontentlength>
                      <d:creationdate>2026-01-02T03:04:05Z</d:creationdate>
                      <d:getlastmodified>Fri, 02 Jan 2026 04:05:06 GMT</d:getlastmodified>
                    </d:prop></d:propstat>
                  </d:response>
                </d:multistatus>
                """;

        var resources = WebDavStorageProviderDefinitionV1.parseMultiStatus(xml.getBytes());

        assertEquals(2, resources.size());
        assertEquals("/dav/gover/", resources.get(0).href());
        assertTrue(resources.get(0).collection());
        assertEquals(42L, resources.get(1).sizeInBytes());
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), resources.get(1).created());
        assertEquals(Instant.parse("2026-01-02T04:05:06Z"), resources.get(1).updated());
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
    void folderExistsRecognizesEncodedHrefWhenBasePathContainsSpecialCharacters() throws StorageException {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents (old), 2026/";

        var client = new FakeWebDavClient();
        client.propfindResponses.put(
                "/dav/gover/documents (old), 2026/",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents%20%28old%29%2C%202026", true, 0L)))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        assertTrue(provider.folderExists(config, "/"));
    }

    @Test
    void retrieveFolderMapsEncodedChildrenWhenBasePathContainsSpecialCharacters() throws StorageException {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents (old), 2026/";

        var client = new FakeWebDavClient();
        client.propfindResponses.put(
                "/dav/gover/documents (old), 2026/",
                Optional.of(List.of(
                        new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents%20%28old%29%2C%202026/", true, 0L),
                        new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents%20%28old%29%2C%202026/sub%20%28draft%29%2C%20A/", true, 0L),
                        new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents%20%28old%29%2C%202026/file%20%28final%29%2C%20A.txt", false, 12L)
                ))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        var folder = provider.retrieveFolder(config, "/", false).orElseThrow();

        assertEquals("/sub (draft), A/", folder.getSubfolders().getFirst().getPathFromRoot());
        assertEquals("/file (final), A.txt", folder.getDocuments().getFirst().getPathFromRoot());
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
    void testConnectionUsesUniqueTemporaryDocumentForWritableCheck() throws StorageException {
        var config = createConfig();
        var client = new FakeWebDavClient();
        client.propfindResponses.put(
                "/dav/",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/", true, 0L)))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        provider.testConnection(config, true);

        assertTrue(client.putIfAbsentUsed);
        assertTrue(client.putUri.getPath().startsWith("/dav/permissions-check-temp-"));
        assertFalse(client.putUri.getPath().equals("/dav/permissions-check-temp"));
        assertEquals(client.putUri, client.deletedUris.get(0));
    }

    @Test
    void testConnectionDoesNotDeleteTemporaryDocumentWhenWritableCheckPutFails() {
        var config = createConfig();
        var client = new FakeWebDavClient();
        client.failPut = true;
        client.propfindResponses.put(
                "/dav/",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/", true, 0L)))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        assertThrows(StorageException.class, () -> provider.testConnection(config, true));

        assertTrue(client.putIfAbsentUsed);
        assertTrue(client.deletedUris.isEmpty());
    }

    @Test
    void retrieveDocumentContentErrorIncludesWebDavPathWithoutUrl() {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents/";

        var client = new FakeWebDavClient();
        client.failGet = true;

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        var exception = assertThrows(StorageException.class, () -> provider.retrieveDocumentContent(config, "/folder/file.txt"));

        assertTrue(exception.getMessage().contains("WebDAV-Pfad:"));
        assertTrue(exception.getMessage().contains("/gover/documents/folder/file.txt"));
        assertFalse(exception.getMessage().contains("https://example.test"));
    }

    @Test
    void createFolderErrorIncludesWebDavPathWithoutUrl() {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents/";

        var client = new FakeWebDavClient();
        client.failMkcol = true;
        client.propfindResponses.put(
                "/dav/gover/documents/",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/", true, 0L)))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        var exception = assertThrows(StorageException.class, () -> provider.createFolder(config, "/folder/"));

        assertTrue(exception.getMessage().contains("WebDAV-Pfad:"));
        assertTrue(exception.getMessage().contains("/gover/documents/folder/"));
        assertFalse(exception.getMessage().contains("https://example.test"));
    }

    @Test
    void moveDocumentErrorIncludesSourceAndTargetWebDavPathsWithoutUrl() {
        var config = createConfig();
        config.baseUrl = "https://example.test/dav/";
        config.basePath = "/gover/documents/";

        var client = new FakeWebDavClient();
        client.failMove = true;
        client.propfindResponses.put(
                "/dav/gover/documents/source.txt",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/source.txt", false, 12L)))
        );
        client.propfindResponses.put(
                "/dav/gover/documents/folder/",
                Optional.of(List.of(new WebDavStorageProviderDefinitionV1.WebDavRemoteResource("/dav/gover/documents/folder/", true, 0L)))
        );

        var provider = createProvider(client, mock(KnownExtensionsService.class));

        var exception = assertThrows(StorageException.class, () -> provider.moveDocument(config, "/source.txt", "/folder/target.txt"));

        assertTrue(exception.getMessage().contains("WebDAV-Quellpfad:"));
        assertTrue(exception.getMessage().contains("/gover/documents/source.txt"));
        assertTrue(exception.getMessage().contains("WebDAV-Zielpfad:"));
        assertTrue(exception.getMessage().contains("/gover/documents/folder/target.txt"));
        assertFalse(exception.getMessage().contains("https://example.test"));
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
        private boolean putIfAbsentUsed;
        private boolean failMkcol;
        private boolean failPut;
        private boolean failGet;
        private boolean failMove;

        private FakeWebDavClient() {
            super("user", "password", httpProperties());
        }

        @Override
        Optional<List<WebDavStorageProviderDefinitionV1.WebDavRemoteResource>> propfind(URI uri, int depth) {
            return propfindResponses.getOrDefault(uri.getPath(), Optional.empty());
        }

        @Override
        void mkcol(URI uri) throws StorageException {
            if (failMkcol) {
                throw new StorageException("mkcol failed");
            }
        }

        @Override
        void put(URI uri, InputStream data, String contentType) throws StorageException {
            putUri = uri;
            putContentType = contentType;
            if (failPut) {
                throw new StorageException("put failed");
            }
        }

        @Override
        void putIfAbsent(URI uri, InputStream data, String contentType) throws StorageException {
            putIfAbsentUsed = true;
            put(uri, data, contentType);
        }

        @Override
        InputStream get(URI uri) throws StorageException {
            if (failGet) {
                throw new StorageException("get failed");
            }
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
        void move(URI sourceUri, URI targetUri) throws StorageException {
            if (failMove) {
                throw new StorageException("move failed");
            }
        }
    }
}

package de.aivot.prosuna.backend.plugins.core.v1.storage;

import de.aivot.prosuna.backend.core.properties.HttpServiceProperties;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.exceptions.StorageException;
import de.aivot.prosuna.backend.storage.models.StorageDocument;
import de.aivot.prosuna.backend.storage.models.StorageFolder;
import de.aivot.prosuna.backend.storage.models.StorageItem;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import de.aivot.prosuna.backend.storage.services.KnownExtensionsService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.storage.utils.StoragePathUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class WebDavStorageProviderDefinitionV1 implements StorageProviderDefinition<WebDavStorageProviderDefinitionV1.Config> {
    private static final String WRITABLE_CHECK_DOCUMENT_PREFIX = "/permissions-check-temp-";

    private static final String PROPFIND_BODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:">
              <d:prop>
                <d:resourcetype/>
                <d:getcontentlength/>
                <d:creationdate/>
                <d:getlastmodified/>
              </d:prop>
            </d:propfind>
            """;

    private final SecretService secretService;
    private final KnownExtensionsService knownExtensionsService;
    private final StorageProviderRepository storageProviderRepository;
    private final HttpServiceProperties httpServiceProperties;

    public WebDavStorageProviderDefinitionV1(SecretService secretService,
                                             KnownExtensionsService knownExtensionsService,
                                             StorageProviderRepository storageProviderRepository,
                                             HttpServiceProperties httpServiceProperties) {
        this.secretService = secretService;
        this.knownExtensionsService = knownExtensionsService;
        this.storageProviderRepository = storageProviderRepository;
        this.httpServiceProperties = httpServiceProperties;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "webdav_storage";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "WebDAV Speicheranbieter";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Speichert Dokumente auf einem WebDAV-kompatiblen Speicher.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Bindet einen über WebDAV erreichbaren Dokumentenspeicher als Prosuna-Speicheranbieter ein.

                Nach der Konfiguration von Serveradresse und Zugangsdaten können Dokumente und Ordner über das WebDAV-Protokoll gelesen, geschrieben und verwaltet werden. Dadurch lassen sich bestehende WebDAV-kompatible Dokumentenablagen in Prozesse und Anlagenverwaltung integrieren.
                """;
    }

    @Nonnull
    @Override
    public Boolean getSupportsMetadataAttributes() {
        return false;
    }

    @Override
    public void validateConfiguration(@Nonnull StorageProviderEntity provider, Config config) throws ResponseException {
        URI providerRootUri;
        try {
            providerRootUri = toWebDavUri(config, "/");
        } catch (StorageException e) {
            throw ResponseException.badRequest(e.getMessage());
        }

        for (var existingProvider : storageProviderRepository.findAllByStorageProviderDefinitionKey(getKey())) {
            if (Objects.equals(existingProvider.getId(), provider.getId())) {
                continue;
            }

            var existingRootUri = getConfiguredRootUri(existingProvider);
            if (existingRootUri.isEmpty()) {
                continue;
            }

            if (rootsOverlap(providerRootUri, existingRootUri.get())) {
                var err = String.format(
                        "Der WebDAV-Basispfad %s überschneidet sich mit dem WebDAV-Basispfad %s des Speicheranbieters %s mit der ID %s.",
                        StringUtils.quote(providerRootUri.toString()),
                        StringUtils.quote(existingRootUri.get().toString()),
                        StringUtils.quote(existingProvider.getName()),
                        StringUtils.quote(String.valueOf(existingProvider.getId()))
                );

                var derivedRuntimeData = new DerivedRuntimeElementData();
                derivedRuntimeData.putError("base_url", err);
                derivedRuntimeData.putError("base_path", err);
                throw ResponseException.badRequest(err, derivedRuntimeData);
            }
        }
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

        return layout;
    }

    @Override
    public Class<WebDavStorageProviderDefinitionV1.Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public void initializeProvider(@Nonnull Config config) throws StorageException {
        testConnection(config, false);
    }

    @Override
    public boolean shouldResync(@Nullable Config oldConfig, @Nonnull Config newConfig) {
        if (oldConfig == null) {
            return true;
        }

        return !Objects.equals(oldConfig.baseUrl, newConfig.baseUrl)
                || !Objects.equals(oldConfig.basePath, newConfig.basePath)
                || !Objects.equals(oldConfig.username, newConfig.username)
                || !Objects.equals(oldConfig.passwordSecret, newConfig.passwordSecret);
    }

    @Override
    public void testConnection(@Nonnull Config config, @Nonnull Boolean mustCheckWritable) throws StorageException {
        if (!folderExists(config, "/")) {
            throw new StorageException("Der WebDAV-Basispfad %s existiert nicht oder ist kein Verzeichnis.", StringUtils.quote(normalizeFolderPath(config.basePath)));
        }

        if (mustCheckWritable) {
            var client = getClient(config);
            var testDocumentPath = createWritableCheckDocumentPath();
            var testDocumentUri = toWebDavUri(config, testDocumentPath);
            var created = false;
            try {
                withWebDavPathContext(
                        config,
                        testDocumentPath,
                        () -> client.putIfAbsent(
                                testDocumentUri,
                                new ByteArrayInputStream(new byte[0]),
                                StorageService.UNKNOWN_MIME_TYPE
                        )
                );
                created = true;
            } finally {
                if (created) {
                    withWebDavPathContext(config, testDocumentPath, () -> client.delete(testDocumentUri));
                }
            }
        }
    }

    @Nonnull
    @Override
    public StorageFolder createFolder(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var normalizedPath = normalizeFolderPath(pathFromRoot);
        if ("/".equals(normalizedPath)) {
            return rootFolder(config);
        }

        var existingFolder = retrieveFolder(config, normalizedPath, false);
        if (existingFolder.isPresent()) {
            return existingFolder.get();
        }

        if (documentExists(config, trimTrailingSlash(normalizedPath))) {
            throw new StorageException("Unter %s existiert bereits ein Dokument.", StringUtils.quote(debugPath(config, normalizedPath)));
        }

        var parentPath = getParentFolderPath(normalizedPath);
        if (!folderExists(config, parentPath)) {
            throw new StorageException("Das übergeordnete WebDAV-Verzeichnis für den Ordner %s existiert nicht.", StringUtils.quote(debugPath(config, normalizedPath)));
        }

        withWebDavPathContext(config, normalizedPath, () -> getClient(config).mkcol(toWebDavUri(config, normalizedPath)));

        return retrieveFolder(config, normalizedPath, false).orElseGet(() -> new StorageFolder(
                normalizedPath,
                StringUtils.getLastPathSegment(normalizedPath),
                new LinkedList<>(),
                new LinkedList<>(),
                false
        ));
    }

    @Nonnull
    @Override
    public Optional<StorageFolder> retrieveFolder(@Nonnull Config config, @Nonnull String pathFromRoot, boolean recursive) throws StorageException {
        var normalizedPath = normalizeFolderPath(pathFromRoot);
        var resources = withWebDavPathContext(config, normalizedPath, () -> getClient(config).propfind(toWebDavUri(config, normalizedPath), 1));
        if (resources.isEmpty()) {
            return Optional.empty();
        }

        var providerResources = withWebDavPathContext(config, normalizedPath, () -> toProviderResources(config, resources.get()));
        var normalizedDocumentPath = trimTrailingSlash(normalizedPath);
        var current = providerResources
                .stream()
                .filter(resource -> normalizedPath.equals(resource.pathFromRoot()) || normalizedDocumentPath.equals(resource.pathFromRoot()))
                .findFirst();
        if (current.isPresent() && !current.get().collection()) {
            return Optional.empty();
        }

        var folder = withTimestamps(new StorageFolder(
                normalizedPath,
                "/".equals(normalizedPath) ? "Root" : StringUtils.getLastPathSegment(normalizedPath),
                new LinkedList<>(),
                new LinkedList<>(),
                recursive
        ), current.map(WebDavResource::created).orElse(null), current.map(WebDavResource::updated).orElse(null));

        for (var resource : providerResources) {
            if (normalizedPath.equals(normalizeFolderPath(resource.pathFromRoot()))) {
                continue;
            }
            if (!isDirectChild(normalizedPath, resource.pathFromRoot())) {
                continue;
            }

            if (resource.collection()) {
                var resourceFolderPath = normalizeFolderPath(resource.pathFromRoot());
                if (recursive) {
                    retrieveFolder(config, resourceFolderPath, true)
                            .ifPresent(folder::addSubfolder);
                } else {
                    folder.addSubfolder(withTimestamps(new StorageFolder(
                            resourceFolderPath,
                            StringUtils.getLastPathSegment(resourceFolderPath),
                            new LinkedList<>(),
                            new LinkedList<>(),
                            false
                    ), resource.created(), resource.updated()));
                }
            } else {
                folder.addDocument(withTimestamps(new StorageDocument(
                        normalizeDocumentPath(resource.pathFromRoot()),
                        StringUtils.getLastPathSegment(resource.pathFromRoot()),
                        resource.sizeInBytes(),
                        StorageItemMetadata.empty()
                ), resource.created(), resource.updated()));
            }
        }

        return Optional.of(folder);
    }

    @Override
    public boolean folderExists(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var resource = retrieveSingleResource(config, normalizeFolderPath(pathFromRoot));
        return resource
                .map(WebDavResource::collection)
                .orElse(false);
    }

    @Nonnull
    @Override
    public StorageFolder moveFolder(@Nonnull Config config,
                                    @Nonnull String sourcePathFromRoot,
                                    @Nonnull String targetPathFromRoot) throws StorageException {
        var normalizedSourcePath = normalizeFolderPath(sourcePathFromRoot);
        var normalizedTargetPath = normalizeFolderPath(targetPathFromRoot);

        if (!folderExists(config, normalizedSourcePath)) {
            throw new StorageException("Der Quellordner %s konnte nicht gefunden werden.", StringUtils.quote(debugPath(config, normalizedSourcePath)));
        }

        if (normalizedSourcePath.equals(normalizedTargetPath)) {
            var debugTargetPath = debugPath(config, normalizedTargetPath);
            return retrieveFolder(config, normalizedTargetPath, true)
                    .orElseThrow(() -> new StorageException("Der Ordner %s konnte nicht abgerufen werden.", StringUtils.quote(debugTargetPath)));
        }

        validateFolderMoveOrCopyTarget(normalizedSourcePath, normalizedTargetPath, config);

        withWebDavPathContext(
                config,
                normalizedSourcePath,
                normalizedTargetPath,
                () -> getClient(config).move(
                        toWebDavUri(config, normalizedSourcePath),
                        toWebDavUri(config, normalizedTargetPath)
                )
        );

        return retrieveFolder(config, normalizedTargetPath, true).orElseGet(() -> new StorageFolder(
                normalizedTargetPath,
                StringUtils.getLastPathSegment(normalizedTargetPath),
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
        var normalizedSourcePath = normalizeFolderPath(sourcePathFromRoot);
        var normalizedTargetPath = normalizeFolderPath(targetPathFromRoot);

        if (!folderExists(config, normalizedSourcePath)) {
            throw new StorageException("Der Quellordner %s konnte nicht gefunden werden.", StringUtils.quote(debugPath(config, normalizedSourcePath)));
        }

        if (normalizedSourcePath.equals(normalizedTargetPath)) {
            var debugTargetPath = debugPath(config, normalizedTargetPath);
            return retrieveFolder(config, normalizedTargetPath, true)
                    .orElseThrow(() -> new StorageException("Der Ordner %s konnte nicht abgerufen werden.", StringUtils.quote(debugTargetPath)));
        }

        validateFolderMoveOrCopyTarget(normalizedSourcePath, normalizedTargetPath, config);

        withWebDavPathContext(
                config,
                normalizedSourcePath,
                normalizedTargetPath,
                () -> getClient(config).copy(
                        toWebDavUri(config, normalizedSourcePath),
                        toWebDavUri(config, normalizedTargetPath)
                )
        );

        return retrieveFolder(config, normalizedTargetPath, true).orElseGet(() -> new StorageFolder(
                normalizedTargetPath,
                StringUtils.getLastPathSegment(normalizedTargetPath),
                new LinkedList<>(),
                new LinkedList<>(),
                true
        ));
    }

    @Override
    public void deleteFolder(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var normalizedPath = normalizeFolderPath(pathFromRoot);
        if ("/".equals(normalizedPath)) {
            throw new StorageException("Das WebDAV-Stammverzeichnis des Speicheranbieters %s kann nicht gelöscht werden.", StringUtils.quote(debugPath(config, normalizedPath)));
        }

        withWebDavPathContext(config, normalizedPath, () -> getClient(config).delete(toWebDavUri(config, normalizedPath)));
    }

    @Nonnull
    @Override
    public StorageDocument storeDocument(@Nonnull Config config,
                                         @Nonnull String pathFromRoot,
                                         @Nonnull InputStream data,
                                         @Nonnull StorageItemMetadata metadata) throws StorageException {
        var normalizedPath = normalizeDocumentPath(pathFromRoot);
        var parentPath = getParentFolderPath(normalizedPath);
        if (!folderExists(config, parentPath)) {
            throw new StorageException("Das übergeordnete WebDAV-Verzeichnis für das Dokument %s existiert nicht.", StringUtils.quote(debugPath(config, normalizedPath)));
        }

        var mimeType = knownExtensionsService
                .determineMimeType(normalizedPath)
                .orElse(StorageService.UNKNOWN_MIME_TYPE);

        withWebDavPathContext(config, normalizedPath, () -> getClient(config).put(toWebDavUri(config, normalizedPath), data, mimeType));

        return retrieveDocument(config, normalizedPath).orElseGet(() -> new StorageDocument(
                normalizedPath,
                StringUtils.getLastPathSegment(normalizedPath),
                0L,
                StorageItemMetadata.empty()
        ));
    }

    @Nonnull
    @Override
    public Optional<StorageDocument> retrieveDocument(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var normalizedPath = normalizeDocumentPath(pathFromRoot);
        var resource = retrieveSingleResource(config, normalizedPath);
        if (resource.isEmpty() || resource.get().collection()) {
            return Optional.empty();
        }

        return Optional.of(withTimestamps(new StorageDocument(
                normalizedPath,
                StringUtils.getLastPathSegment(normalizedPath),
                resource.get().sizeInBytes(),
                StorageItemMetadata.empty()
        ), resource.get().created(), resource.get().updated()));
    }

    @Nonnull
    @Override
    public InputStream retrieveDocumentContent(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var normalizedPath = normalizeDocumentPath(pathFromRoot);
        return withWebDavPathContext(config, normalizedPath, () -> getClient(config).get(toWebDavUri(config, normalizedPath)));
    }

    @Override
    public boolean documentExists(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var resource = retrieveSingleResource(config, normalizeDocumentPath(pathFromRoot));
        return resource
                .map(webDavResource -> !webDavResource.collection())
                .orElse(false);
    }

    @Nonnull
    @Override
    public StorageDocument moveDocument(@Nonnull Config config,
                                        @Nonnull String sourcePathFromRoot,
                                        @Nonnull String targetPathFromRoot) throws StorageException {
        var normalizedSourcePath = normalizeDocumentPath(sourcePathFromRoot);
        var normalizedTargetPath = normalizeDocumentPath(targetPathFromRoot);

        if (!documentExists(config, normalizedSourcePath)) {
            throw new StorageException("Das Quelldokument %s konnte nicht gefunden werden.", StringUtils.quote(debugPath(config, normalizedSourcePath)));
        }

        if (normalizedSourcePath.equals(normalizedTargetPath)) {
            var debugSourcePath = debugPath(config, normalizedSourcePath);
            return retrieveDocument(config, normalizedSourcePath)
                    .orElseThrow(() -> new StorageException("Das Quelldokument %s konnte nicht gefunden werden.", StringUtils.quote(debugSourcePath)));
        }

        validateDocumentTargetParent(config, normalizedTargetPath);

        withWebDavPathContext(
                config,
                normalizedSourcePath,
                normalizedTargetPath,
                () -> getClient(config).move(
                        toWebDavUri(config, normalizedSourcePath),
                        toWebDavUri(config, normalizedTargetPath)
                )
        );

        return retrieveDocument(config, normalizedTargetPath).orElseGet(() -> new StorageDocument(
                normalizedTargetPath,
                StringUtils.getLastPathSegment(normalizedTargetPath),
                0L,
                StorageItemMetadata.empty()
        ));
    }

    @Nonnull
    @Override
    public StorageDocument copyDocument(@Nonnull Config config,
                                        @Nonnull String sourcePathFromRoot,
                                        @Nonnull String targetPathFromRoot) throws StorageException {
        var normalizedSourcePath = normalizeDocumentPath(sourcePathFromRoot);
        var normalizedTargetPath = normalizeDocumentPath(targetPathFromRoot);

        if (!documentExists(config, normalizedSourcePath)) {
            throw new StorageException("Das Quelldokument %s konnte nicht gefunden werden.", StringUtils.quote(debugPath(config, normalizedSourcePath)));
        }

        if (normalizedSourcePath.equals(normalizedTargetPath)) {
            var debugSourcePath = debugPath(config, normalizedSourcePath);
            return retrieveDocument(config, normalizedSourcePath)
                    .orElseThrow(() -> new StorageException("Das Quelldokument %s konnte nicht gefunden werden.", StringUtils.quote(debugSourcePath)));
        }

        validateDocumentTargetParent(config, normalizedTargetPath);

        withWebDavPathContext(
                config,
                normalizedSourcePath,
                normalizedTargetPath,
                () -> getClient(config).copy(
                        toWebDavUri(config, normalizedSourcePath),
                        toWebDavUri(config, normalizedTargetPath)
                )
        );

        return retrieveDocument(config, normalizedTargetPath).orElseGet(() -> new StorageDocument(
                normalizedTargetPath,
                StringUtils.getLastPathSegment(normalizedTargetPath),
                0L,
                StorageItemMetadata.empty()
        ));
    }

    @Override
    public void deleteDocument(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var normalizedPath = normalizeDocumentPath(pathFromRoot);
        withWebDavPathContext(config, normalizedPath, () -> getClient(config).delete(toWebDavUri(config, normalizedPath)));
    }

    private static <T> T withWebDavPathContext(@Nonnull Config config,
                                               @Nonnull String pathFromRoot,
                                               @Nonnull WebDavStorageOperation<T> operation) throws StorageException {
        try {
            return operation.run();
        } catch (StorageException e) {
            throw appendWebDavPathContext(e, config, pathFromRoot);
        }
    }

    private static void withWebDavPathContext(@Nonnull Config config,
                                              @Nonnull String pathFromRoot,
                                              @Nonnull WebDavStorageAction action) throws StorageException {
        try {
            action.run();
        } catch (StorageException e) {
            throw appendWebDavPathContext(e, config, pathFromRoot);
        }
    }

    private static void withWebDavPathContext(@Nonnull Config config,
                                              @Nonnull String sourcePathFromRoot,
                                              @Nonnull String targetPathFromRoot,
                                              @Nonnull WebDavStorageAction action) throws StorageException {
        try {
            action.run();
        } catch (StorageException e) {
            throw appendWebDavPathContext(e, config, sourcePathFromRoot, targetPathFromRoot);
        }
    }

    @Nonnull
    private static StorageException appendWebDavPathContext(@Nonnull StorageException e,
                                                            @Nonnull Config config,
                                                            @Nonnull String pathFromRoot) {
        if (hasWebDavPathContext(e)) {
            return e;
        }

        try {
            return new StorageException(
                    e,
                    "%s WebDAV-Pfad: %s.",
                    e.getMessage(),
                    StringUtils.quote(debugPath(config, pathFromRoot))
            );
        } catch (StorageException ignored) {
            return e;
        }
    }

    @Nonnull
    private static StorageException appendWebDavPathContext(@Nonnull StorageException e,
                                                            @Nonnull Config config,
                                                            @Nonnull String sourcePathFromRoot,
                                                            @Nonnull String targetPathFromRoot) {
        if (hasWebDavPathContext(e)) {
            return e;
        }

        try {
            return new StorageException(
                    e,
                    "%s WebDAV-Quellpfad: %s. WebDAV-Zielpfad: %s.",
                    e.getMessage(),
                    StringUtils.quote(debugPath(config, sourcePathFromRoot)),
                    StringUtils.quote(debugPath(config, targetPathFromRoot))
            );
        } catch (StorageException ignored) {
            return e;
        }
    }

    private static boolean hasWebDavPathContext(@Nonnull StorageException e) {
        var message = e.getMessage();
        return message != null
                && (message.contains("WebDAV-Pfad:")
                || message.contains("WebDAV-Quellpfad:")
                || message.contains("WebDAV-Zielpfad:"));
    }

    @Nonnull
    private static String debugPath(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var basePath = normalizeFolderPath(config.basePath);
        var providerPath = pathFromRoot.endsWith("/") || "/".equals(pathFromRoot)
                ? normalizeFolderPath(pathFromRoot)
                : normalizeDocumentPath(pathFromRoot);
        return basePath + providerPath.substring(1);
    }

    WebDavClient getClient(@Nonnull Config config) throws StorageException {
        UUID secretUUID;
        try {
            secretUUID = UUID.fromString(config.passwordSecret);
        } catch (Exception e) {
            throw new StorageException("Das Passwort-Geheimnis ist ungültig.");
        }

        SecretEntity secret = secretService
                .retrieve(secretUUID)
                .orElseThrow(() -> new StorageException("Das Geheimnis für das WebDAV-Passwort wurde nicht gefunden."));

        String password;
        try {
            password = secretService.decrypt(secret);
        } catch (Exception e) {
            throw new StorageException(e, "Fehler beim Entschlüsseln des Geheimnisses für das WebDAV-Passwort.");
        }

        return new WebDavClient(config.username, password, httpServiceProperties);
    }

    private void validateFolderMoveOrCopyTarget(@Nonnull String normalizedSourcePath,
                                                @Nonnull String normalizedTargetPath,
                                                @Nonnull Config config) throws StorageException {
        if (normalizedTargetPath.startsWith(normalizedSourcePath) && !normalizedTargetPath.equals(normalizedSourcePath)) {
            throw new StorageException(
                    "Der Zielordner %s darf nicht innerhalb des Quellordners %s liegen.",
                    StringUtils.quote(debugPath(config, normalizedTargetPath)),
                    StringUtils.quote(debugPath(config, normalizedSourcePath))
            );
        }

        var targetParentPath = getParentFolderPath(normalizedTargetPath);
        if (!folderExists(config, targetParentPath)) {
            throw new StorageException("Das Zielverzeichnis für den Ordner %s existiert nicht.", StringUtils.quote(debugPath(config, normalizedTargetPath)));
        }
    }

    private void validateDocumentTargetParent(@Nonnull Config config,
                                              @Nonnull String normalizedTargetPath) throws StorageException {
        var targetParentPath = getParentFolderPath(normalizedTargetPath);
        if (!folderExists(config, targetParentPath)) {
            throw new StorageException("Das Zielverzeichnis für das Dokument %s existiert nicht.", StringUtils.quote(debugPath(config, normalizedTargetPath)));
        }
    }

    @Nonnull
    private Optional<WebDavResource> retrieveSingleResource(@Nonnull Config config,
                                                            @Nonnull String pathFromRoot) throws StorageException {
        var resources = withWebDavPathContext(config, pathFromRoot, () -> getClient(config).propfind(toWebDavUri(config, pathFromRoot), 0));
        if (resources.isEmpty()) {
            return Optional.empty();
        }

        var normalizedPath = pathFromRoot.endsWith("/") ? normalizeFolderPath(pathFromRoot) : normalizeDocumentPath(pathFromRoot);
        return withWebDavPathContext(config, normalizedPath, () -> toProviderResources(config, resources.get()))
                .stream()
                .filter(resource -> resource.pathFromRoot().equals(normalizedPath))
                .findFirst();
    }

    @Nonnull
    private List<WebDavResource> toProviderResources(@Nonnull Config config,
                                                     @Nonnull List<WebDavRemoteResource> remoteResources) throws StorageException {
        var rootPath = normalizeFolderPath(toWebDavUri(config, "/").getPath());
        var result = new LinkedList<WebDavResource>();
        for (var remoteResource : remoteResources) {
            var remotePath = hrefToNormalizedPath(remoteResource);
            if (!remotePath.startsWith(rootPath)) {
                continue;
            }

            var relativePath = remotePath.substring(rootPath.length());
            var providerPath = "/" + relativePath;
            if (remoteResource.collection()) {
                providerPath = normalizeFolderPath(providerPath);
            } else {
                providerPath = normalizeDocumentPath(providerPath);
            }

            result.add(new WebDavResource(providerPath, remoteResource.collection(), remoteResource.sizeInBytes(), remoteResource.created(), remoteResource.updated()));
        }
        return result;
    }

    @Nonnull
    static URI toWebDavUri(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var baseUri = normalizeBaseUri(config.baseUrl);
        var baseUriPath = normalizeFolderPath(baseUri.getPath());
        var basePath = normalizeFolderPath(config.basePath);
        var providerPath = pathFromRoot.endsWith("/") || "/".equals(pathFromRoot)
                ? normalizeFolderPath(pathFromRoot)
                : normalizeDocumentPath(pathFromRoot);
        var combinedPath = baseUriPath + basePath.substring(1) + providerPath.substring(1);
        combinedPath = combinedPath.replaceAll("/{2,}", "/");

        try {
            return new URI(
                    baseUri.getScheme(),
                    baseUri.getUserInfo(),
                    baseUri.getHost(),
                    baseUri.getPort(),
                    combinedPath,
                    null,
                    null
            );
        } catch (URISyntaxException e) {
            throw new StorageException(e, "Die WebDAV-URL %s ist ungültig.", StringUtils.quote(config.baseUrl));
        }
    }

    @Nonnull
    static URI normalizeBaseUri(@Nullable String baseUrl) throws StorageException {
        if (StringUtils.isNullOrEmpty(baseUrl)) {
            throw new StorageException("Die WebDAV-Basis-URL darf nicht leer sein.");
        }

        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (Exception e) {
            throw new StorageException("Die WebDAV-Basis-URL %s ist ungültig.", StringUtils.quote(baseUrl));
        }

        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new StorageException("Die WebDAV-Basis-URL muss mit http:// oder https:// beginnen.");
        }
        if (StringUtils.isNullOrEmpty(uri.getHost())) {
            throw new StorageException("Die WebDAV-Basis-URL %s enthält keinen Host.", StringUtils.quote(baseUrl));
        }
        if (uri.getUserInfo() != null) {
            throw new StorageException("Die WebDAV-Basis-URL darf keine Zugangsdaten enthalten.");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new StorageException("Die WebDAV-Basis-URL darf keine Query-Parameter oder Fragmente enthalten.");
        }

        return uri;
    }

    @Nonnull
    static String normalizeFolderPath(@Nullable String path) throws StorageException {
        return StoragePathUtils.normalizeFolderPath(path);
    }

    @Nonnull
    static String normalizeDocumentPath(@Nullable String path) throws StorageException {
        return StoragePathUtils.normalizeDocumentPath(path);
    }

    @Nonnull
    private static String trimTrailingSlash(@Nonnull String path) {
        return StoragePathUtils.trimTrailingSlash(path);
    }

    @Nonnull
    private static String getParentFolderPath(@Nonnull String path) throws StorageException {
        var normalizedPath = path.endsWith("/") ? normalizeFolderPath(path) : normalizeDocumentPath(path);
        var withoutTrailingSlash = trimTrailingSlash(normalizedPath);
        var lastSlash = withoutTrailingSlash.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return withoutTrailingSlash.substring(0, lastSlash + 1);
    }

    private static boolean isDirectChild(@Nonnull String parentFolderPath,
                                         @Nonnull String childPath) throws StorageException {
        var normalizedParent = normalizeFolderPath(parentFolderPath);
        if (!childPath.startsWith(normalizedParent) || normalizedParent.equals(childPath)) {
            return false;
        }

        var relativePath = childPath.substring(normalizedParent.length());
        if (relativePath.isEmpty()) {
            return false;
        }

        var withoutTrailingSlash = trimTrailingSlash(relativePath);
        return !withoutTrailingSlash.contains("/");
    }

    @Nonnull
    private static String createWritableCheckDocumentPath() {
        return WRITABLE_CHECK_DOCUMENT_PREFIX + UUID.randomUUID();
    }

    @Nonnull
    private static String hrefToPath(@Nonnull String href) {
        try {
            return URI.create(href).getPath();
        } catch (Exception ignored) {
            return href;
        }
    }

    @Nonnull
    private static String hrefToNormalizedPath(@Nonnull WebDavRemoteResource remoteResource) throws StorageException {
        var path = hrefToPath(remoteResource.href());
        return remoteResource.collection()
                ? normalizeFolderPath(path)
                : normalizeDocumentPath(path);
    }

    @Nonnull
    private Optional<URI> getConfiguredRootUri(@Nonnull StorageProviderEntity provider) {
        var rawBaseUrl = provider.getConfiguration().get("base_url");
        var rawBasePath = provider.getConfiguration().get("base_path");
        if (!(rawBaseUrl instanceof String baseUrl)) {
            return Optional.empty();
        }

        var config = new Config();
        config.baseUrl = baseUrl;
        config.basePath = rawBasePath instanceof String basePath ? basePath : "/";

        try {
            return Optional.of(toWebDavUri(config, "/"));
        } catch (StorageException e) {
            return Optional.empty();
        }
    }

    private static boolean rootsOverlap(@Nonnull URI left,
                                        @Nonnull URI right) {
        if (!Objects.equals(left.getScheme(), right.getScheme())
                || !left.getHost().equalsIgnoreCase(right.getHost())
                || effectivePort(left) != effectivePort(right)) {
            return false;
        }

        var leftPath = left.getPath();
        var rightPath = right.getPath();
        return leftPath.startsWith(rightPath) || rightPath.startsWith(leftPath);
    }

    private static int effectivePort(@Nonnull URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return -1;
    }

    @FunctionalInterface
    private interface WebDavStorageOperation<T> {
        T run() throws StorageException;
    }

    @FunctionalInterface
    private interface WebDavStorageAction {
        void run() throws StorageException;
    }

    record WebDavResource(String pathFromRoot, boolean collection, long sizeInBytes, @Nullable Instant created, @Nullable Instant updated) {
    }

    record WebDavRemoteResource(String href, boolean collection, long sizeInBytes, @Nullable Instant created, @Nullable Instant updated) {
        WebDavRemoteResource(String href, boolean collection, long sizeInBytes) {
            this(href, collection, sizeInBytes, null, null);
        }
    }

    static class WebDavClient {
        private final HttpClient httpClient;
        private final String authorizationHeader;
        private final Duration readTimeout;

        WebDavClient(@Nonnull String username,
                     @Nonnull String password,
                     @Nonnull HttpServiceProperties httpServiceProperties) {
            this.httpClient = HttpClient
                    .newBuilder()
                    .connectTimeout(Duration.ofSeconds(httpServiceProperties.getConnectionTimeoutSeconds()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            this.authorizationHeader = createBasicAuthorizationHeader(username, password);
            this.readTimeout = Duration.ofSeconds(httpServiceProperties.getReadTimeoutSeconds());
        }

        @Nonnull
        Optional<List<WebDavRemoteResource>> propfind(@Nonnull URI uri, int depth) throws StorageException {
            var request = requestBuilder(uri)
                    .header("Depth", String.valueOf(depth))
                    .header("Content-Type", "application/xml; charset=utf-8")
                    .method("PROPFIND", HttpRequest.BodyPublishers.ofString(PROPFIND_BODY, StandardCharsets.UTF_8))
                    .build();

            var response = sendBytes(request);
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 207) {
                throw unexpectedStatus(response, "WebDAV-Verzeichnisinformationen konnten nicht abgerufen werden.");
            }

            return Optional.of(parseMultiStatus(response.body()));
        }

        void mkcol(@Nonnull URI uri) throws StorageException {
            var request = requestBuilder(uri)
                    .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                    .build();

            var response = sendBytes(request);
            if (response.statusCode() == 201 || response.statusCode() == 405) {
                return;
            }
            if (response.statusCode() == 409) {
                throw new StorageException("Das übergeordnete WebDAV-Verzeichnis existiert nicht.");
            }
            throw unexpectedStatus(response, "Das WebDAV-Verzeichnis konnte nicht erstellt werden.");
        }

        void put(@Nonnull URI uri,
                 @Nonnull InputStream data,
                 @Nonnull String contentType) throws StorageException {
            put(uri, data, contentType, true);
        }

        void putIfAbsent(@Nonnull URI uri,
                         @Nonnull InputStream data,
                         @Nonnull String contentType) throws StorageException {
            put(uri, data, contentType, false);
        }

        private void put(@Nonnull URI uri,
                         @Nonnull InputStream data,
                         @Nonnull String contentType,
                         boolean overwrite) throws StorageException {
            var requestBuilder = requestBuilder(uri)
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> data));
            if (!overwrite) {
                requestBuilder.header("If-None-Match", "*");
            }

            var request = requestBuilder.build();
            var response = sendBytes(request);
            if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204) {
                return;
            }
            if (!overwrite && response.statusCode() == 412) {
                throw new StorageException("Das temporäre WebDAV-Prüfdokument existiert bereits.");
            }
            throw unexpectedStatus(response, "Das Dokument konnte nicht im WebDAV-Speicher gespeichert werden.");
        }

        @Nonnull
        InputStream get(@Nonnull URI uri) throws StorageException {
            var request = requestBuilder(uri)
                    .GET()
                    .build();

            HttpResponse<InputStream> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (IOException e) {
                throw new StorageException(e, "Die Verbindung zum WebDAV-Speicher konnte nicht hergestellt werden.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StorageException(e, "Die Verbindung zum WebDAV-Speicher wurde unterbrochen.");
            }

            if (response.statusCode() == 200) {
                return response.body();
            }

            try {
                response.body().close();
            } catch (IOException ignored) {
                // The response body is discarded after a failed GET.
            }

            if (response.statusCode() == 404) {
                throw new StorageException("Das Dokument konnte im WebDAV-Speicher nicht gefunden werden.");
            }
            throw new StorageException("WebDAV-Speicher antwortete mit Status %d.", response.statusCode());
        }

        void delete(@Nonnull URI uri) throws StorageException {
            var request = requestBuilder(uri)
                    .DELETE()
                    .build();

            var response = sendBytes(request);
            if (response.statusCode() == 200 || response.statusCode() == 202 || response.statusCode() == 204 || response.statusCode() == 404) {
                return;
            }
            throw unexpectedStatus(response, "Das WebDAV-Element konnte nicht gelöscht werden.");
        }

        void copy(@Nonnull URI sourceUri,
                  @Nonnull URI targetUri) throws StorageException {
            copyOrMove("COPY", sourceUri, targetUri);
        }

        void move(@Nonnull URI sourceUri,
                  @Nonnull URI targetUri) throws StorageException {
            copyOrMove("MOVE", sourceUri, targetUri);
        }

        private void copyOrMove(@Nonnull String method,
                                @Nonnull URI sourceUri,
                                @Nonnull URI targetUri) throws StorageException {
            var request = requestBuilder(sourceUri)
                    .header("Destination", targetUri.toString())
                    .header("Overwrite", "T")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();

            var response = sendBytes(request);
            if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204) {
                return;
            }
            if (response.statusCode() == 404) {
                throw new StorageException("Das WebDAV-Quellelement konnte nicht gefunden werden.");
            }
            throw unexpectedStatus(response, "Das WebDAV-Element konnte nicht kopiert oder verschoben werden.");
        }

        @Nonnull
        private HttpRequest.Builder requestBuilder(@Nonnull URI uri) {
            return HttpRequest
                    .newBuilder(uri)
                    .timeout(readTimeout)
                    .header("Authorization", authorizationHeader);
        }

        @Nonnull
        private HttpResponse<byte[]> sendBytes(@Nonnull HttpRequest request) throws StorageException {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException e) {
                throw new StorageException(e, "Die Verbindung zum WebDAV-Speicher konnte nicht hergestellt werden.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StorageException(e, "Die Verbindung zum WebDAV-Speicher wurde unterbrochen.");
            }
        }

        @Nonnull
        private static StorageException unexpectedStatus(@Nonnull HttpResponse<byte[]> response,
                                                         @Nonnull String message) {
            return new StorageException("%s WebDAV-Speicher antwortete mit Status %d.", message, response.statusCode());
        }

        @Nonnull
        private static String createBasicAuthorizationHeader(@Nonnull String username,
                                                            @Nonnull String password) {
            var credentials = username + ":" + password;
            return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nonnull
    static List<WebDavRemoteResource> parseMultiStatus(@Nonnull byte[] body) throws StorageException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // WebDAV responses come from remote systems, so external XML entities must stay disabled.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            var builder = factory.newDocumentBuilder();
            var document = builder.parse(new InputSource(new ByteArrayInputStream(body)));
            var responses = document.getElementsByTagNameNS("*", "response");
            var result = new LinkedList<WebDavRemoteResource>();
            for (var i = 0; i < responses.getLength(); i++) {
                var response = responses.item(i);
                if (!(response instanceof Element responseElement)) {
                    continue;
                }

                var href = textContent(responseElement, "href").orElse(null);
                if (StringUtils.isNullOrEmpty(href)) {
                    continue;
                }

                var collection = hasDescendant(responseElement, "collection");
                var sizeInBytes = textContent(responseElement, "getcontentlength")
                        .map(WebDavStorageProviderDefinitionV1::parseLongOrZero)
                        .orElse(0L);
                var created = textContent(responseElement, "creationdate")
                        .flatMap(WebDavStorageProviderDefinitionV1::parseIsoInstant)
                        .orElse(null);
                var updated = textContent(responseElement, "getlastmodified")
                        .flatMap(WebDavStorageProviderDefinitionV1::parseRfc1123Instant)
                        .orElse(null);

                result.add(new WebDavRemoteResource(href, collection, sizeInBytes, created, updated));
            }
            return result;
        } catch (Exception e) {
            throw new StorageException(e, "Die WebDAV-Verzeichnisantwort konnte nicht verarbeitet werden.");
        }
    }

    @Nonnull
    private static Optional<String> textContent(@Nonnull Element element,
                                                @Nonnull String localName) {
        var nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return Optional.empty();
        }
        var value = nodes.item(0).getTextContent();
        return StringUtils.isNullOrEmpty(value) ? Optional.empty() : Optional.of(value.trim());
    }

    private static boolean hasDescendant(@Nonnull Element element,
                                         @Nonnull String localName) {
        var nodes = element.getElementsByTagNameNS("*", localName);
        for (var i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private static long parseLongOrZero(@Nonnull String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    @Nonnull
    private static Optional<Instant> parseIsoInstant(@Nonnull String value) {
        try {
            return Optional.of(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    @Nonnull
    private static Optional<Instant> parseRfc1123Instant(@Nonnull String value) {
        try {
            return Optional.of(ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    @Nonnull
    private static <T extends StorageItem> T withTimestamps(@Nonnull T item,
                                                           @Nullable Instant created,
                                                           @Nullable Instant updated) {
        if (created != null) {
            item.setCreated(created);
        }
        if (updated != null) {
            item.setUpdated(updated);
        }
        return item;
    }

    @LayoutElementPOJOBinding(id = "config", type = ElementType.ConfigLayout)
    public static class Config {
        @InputElementPOJOBinding(id = "base_url", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Basis-URL"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die URL des WebDAV-Speichers."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8)
        })
        public String baseUrl;

        @InputElementPOJOBinding(id = "base_path", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Basispfad"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Pfad innerhalb des WebDAV-Speichers, der als Stammverzeichnis verwendet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4)
        })
        public String basePath;

        @InputElementPOJOBinding(id = "username", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Benutzername"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Benutzername für den Zugriff auf den WebDAV-Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6)
        })
        public String username;

        @InputElementPOJOBinding(id = "password_secret", type = ElementType.SecretSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Passwort"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Geheimnis des Passworts für den Zugriff auf den WebDAV-Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6)
        })
        public String passwordSecret;
    }
}

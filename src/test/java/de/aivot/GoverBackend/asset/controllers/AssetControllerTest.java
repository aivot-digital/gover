package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.dtos.AssetCreateRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetMoveRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetRequestDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.GoverBackend.asset.permissions.AssetPermissionProvider;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.audit.services.AuditLogService;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.services.StorageProviderService;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssetControllerTest {
    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserService userService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private StorageProviderService storageProviderService;

    @Mock
    private VStorageIndexItemWithAssetRepository storageIndexItemWithAssetRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private HttpServletRequest request;

    private AssetController assetController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        assetController = new AssetController(
                new AuditService(auditLogService),
                userService,
                permissionService,
                storageProviderService,
                storageIndexItemWithAssetRepository,
                storageService,
                assetRepository
        );
        jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
    }

    @Test
    void deleteFolder_DeletesProviderScopedFolderWithoutDirectAssetDeletion() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets);

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/assets/42/folders/images/"));

        assetController.deleteFolder(jwt, 42, request);

        verify(permissionService).testSystemPermission("user-1", AssetPermissionProvider.ASSET_DELETE);
        verify(storageService).deleteFolder(42, "/images/");
        verifyNoInteractions(assetRepository);
        verify(auditLogService).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteFolder_DeletesRootFolderWithoutDirectAssetDeletion() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets);

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/assets/42/folders/"));

        assetController.deleteFolder(jwt, 42, request);

        verify(storageService).deleteFolder(42, "/");
        verifyNoInteractions(assetRepository);
    }

    @Test
    void createFile_RejectsIfFileAlreadyExists() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets)
                .setReadOnlyStorage(false);
        MultipartFile newFile = new MockMultipartFile(
                "file",
                "existing.pdf",
                "application/pdf",
                "content".getBytes()
        );

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(42, "/images/existing.pdf"))
                .thenReturn(Optional.empty());
        when(storageService.getDocument(42, "/images/existing.pdf"))
                .thenReturn(Optional.of(new StorageDocument(
                        "/images/existing.pdf",
                        "existing.pdf",
                        7L,
                        StorageItemMetadata.empty()
                )));
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/assets/42/files/images/existing.pdf"));

        var responseException = assertThrows(
                ResponseException.class,
                () -> assetController.createFile(
                        jwt,
                        42,
                        newFile,
                        new AssetCreateRequestDTO(false, StorageItemMetadata.empty()),
                        request
                )
        );

        assertEquals("Am angegebenen Pfad existiert bereits eine Datei.", responseException.getTitle());
        verify(storageService, never()).storeDocument(eq(42), eq("/images/existing.pdf"), any(InputStream.class), any(StorageItemMetadata.class));
    }

    @Test
    void updateFile_RejectsReplacementWithDifferentExtension() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets)
                .setReadOnlyStorage(false);
        var asset = new AssetEntity()
                .setKey(UUID.randomUUID())
                .setStorageProviderId(42)
                .setStoragePathFromRoot("/images/existing.pdf")
                .setPrivate(false);
        MultipartFile replacementFile = new MockMultipartFile(
                "file",
                "replacement.png",
                "image/png",
                "updated-content".getBytes()
        );

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(42, "/images/existing.pdf"))
                .thenReturn(Optional.of(asset));
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/assets/42/files/images/existing.pdf"));

        var responseException = assertThrows(
                ResponseException.class,
                () -> assetController.updateFile(jwt, 42, replacementFile, new AssetRequestDTO(true, null), request)
        );

        assertEquals("Der Dateiinhalt kann nur durch eine Datei mit derselben Dateiendung ersetzt werden.", responseException.getTitle());
        verify(storageService, never()).storeDocument(eq(42), eq("/images/existing.pdf"), any(InputStream.class), any(StorageItemMetadata.class));
    }

    @Test
    void updateFile_AllowsReplacementWithSameExtensionIgnoringCase() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets)
                .setReadOnlyStorage(false);
        var asset = new AssetEntity()
                .setKey(UUID.randomUUID())
                .setStorageProviderId(42)
                .setStoragePathFromRoot("/images/existing.pdf")
                .setPrivate(false);
        var updatedIndexItem = new VStorageIndexItemWithAssetEntity()
                .setStorageProviderId(42)
                .setPathFromRoot("/images/existing.pdf")
                .setFilename("existing.pdf")
                .setMimeType("application/pdf")
                .setMetadata(StorageItemMetadata.empty());
        MultipartFile replacementFile = new MockMultipartFile(
                "file",
                "replacement.PDF",
                "application/pdf",
                "updated-content".getBytes()
        );

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(42, "/images/existing.pdf"))
                .thenReturn(Optional.of(asset));
        when(storageIndexItemWithAssetRepository.findById(VStorageIndexItemWithAssetEntityId.of(42, "/images/existing.pdf")))
                .thenReturn(Optional.of(updatedIndexItem));
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/assets/42/files/images/existing.pdf"));

        var result = assetController.updateFile(jwt, 42, replacementFile, new AssetRequestDTO(true, null), request);

        assertEquals(updatedIndexItem, result);
        verify(storageService).storeDocument(eq(42), eq("/images/existing.pdf"), any(InputStream.class), eq(StorageItemMetadata.empty()));
    }

    @Test
    void moveFile_RejectsIfTargetFileAlreadyExistsInStorage() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets)
                .setReadOnlyStorage(false);

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(42, "/images/target.pdf"))
                .thenReturn(Optional.empty());
        when(storageService.getDocument(42, "/images/target.pdf"))
                .thenReturn(Optional.of(new StorageDocument(
                        "/images/target.pdf",
                        "target.pdf",
                        7L,
                        StorageItemMetadata.empty()
                )));

        var responseException = assertThrows(
                ResponseException.class,
                () -> assetController.moveFile(jwt, 42, new AssetMoveRequestDTO("/images/source.pdf", "/images/target.pdf"))
        );

        assertEquals("Am Zielpfad existiert bereits eine Datei.", responseException.getTitle());
        verify(storageService, never()).moveDocument(42, "/images/source.pdf", "/images/target.pdf");
    }

    @Test
    void updateFileMetadata_UpdatesMetadataInStorage() throws Exception {
        var execUser = new UserEntity()
                .setId("user-1")
                .setFullName("Jane Doe");
        var storageProvider = new StorageProviderEntity()
                .setId(42)
                .setType(StorageProviderType.Assets)
                .setReadOnlyStorage(false);
        var asset = new AssetEntity()
                .setKey(UUID.randomUUID())
                .setStorageProviderId(42)
                .setStoragePathFromRoot("/images/existing.pdf")
                .setPrivate(false);
        var existingMetadata = StorageItemMetadata.empty();
        existingMetadata.put("x-amz-meta-color", "red");
        var updatedMetadata = StorageItemMetadata.empty();
        updatedMetadata.put("x-amz-meta-color", "blue");
        var updatedIndexItem = new VStorageIndexItemWithAssetEntity()
                .setStorageProviderId(42)
                .setPathFromRoot("/images/existing.pdf")
                .setFilename("existing.pdf")
                .setMimeType("application/pdf")
                .setMetadata(updatedMetadata);

        when(userService.fromJWT(jwt)).thenReturn(Optional.of(execUser));
        when(storageProviderService.retrieve(42)).thenReturn(Optional.of(storageProvider));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(42, "/images/existing.pdf"))
                .thenReturn(Optional.of(asset));
        when(storageIndexItemWithAssetRepository.findById(VStorageIndexItemWithAssetEntityId.of(42, "/images/existing.pdf")))
                .thenReturn(Optional.of(new VStorageIndexItemWithAssetEntity().setMetadata(existingMetadata)))
                .thenReturn(Optional.of(updatedIndexItem));
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/assets/42/files-metadata/images/existing.pdf"));

        var result = assetController.updateFileMetadata(jwt, 42, updatedMetadata, request);

        assertEquals(updatedIndexItem, result);
        verify(storageService).updateDocumentMetadata(42, "/images/existing.pdf", updatedMetadata);
    }
}

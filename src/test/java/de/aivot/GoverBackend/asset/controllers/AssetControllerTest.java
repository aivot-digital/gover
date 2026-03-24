package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.permissions.AssetPermissionProvider;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.audit.services.AuditLogService;
import de.aivot.GoverBackend.audit.services.AuditService;
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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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
}

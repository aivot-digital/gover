package de.aivot.gover.backend.storage.controllers;

import de.aivot.gover.backend.audit.services.AuditLogService;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.storage.permissions.StoragePermissionProvider;
import de.aivot.gover.backend.storage.repositories.StorageIndexItemRepository;
import de.aivot.gover.backend.storage.services.StorageProviderConfigurationService;
import de.aivot.gover.backend.storage.services.StorageProviderDefinitionService;
import de.aivot.gover.backend.storage.services.StorageProviderService;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.user.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageProviderControllerTest {
    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserService userService;

    @Mock
    private StorageProviderService storageProviderService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StorageIndexItemRepository storageIndexItemRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageProviderDefinitionService storageProviderDefinitionService;

    @Mock
    private StorageProviderConfigurationService storageProviderConfigurationService;

    @Mock
    private HttpServletRequest request;

    private StorageProviderController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new StorageProviderController(
                new AuditService(auditLogService),
                userService,
                storageProviderService,
                permissionService,
                rabbitTemplate,
                storageIndexItemRepository,
                storageService,
                storageProviderDefinitionService,
                storageProviderConfigurationService
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
    void getFolderPreservesPlusInFolderPath() throws Exception {
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/storage-providers/42/folders/folder+a/"));
        when(storageIndexItemRepository.listAllInFolder(42, "/folder+a/", false))
                .thenReturn(List.of());

        controller.getFolder(jwt, 42, request);

        verify(permissionService)
                .hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ);
        verify(storageIndexItemRepository)
                .listAllInFolder(42, "/folder+a/", false);
    }
}

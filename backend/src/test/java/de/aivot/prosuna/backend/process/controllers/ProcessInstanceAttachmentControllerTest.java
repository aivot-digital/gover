package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceAttachmentControllerTest {
    private static final UUID ATTACHMENT_KEY = UUID.fromString("5e7a1f8c-2d64-4a8b-9a31-0f6d2c4b7e19");

    private final Jwt jwt = mock(Jwt.class);
    private final UserService userService = mock(UserService.class);
    private final ProcessInstanceAttachmentService attachmentService = mock(ProcessInstanceAttachmentService.class);
    private final StorageService storageService = mock(StorageService.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private ProcessInstanceAttachmentController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(storageService.getDocumentContent(3, "/attachments/file"))
                .thenReturn(new ByteArrayInputStream(new byte[0]));
        controller = new ProcessInstanceAttachmentController(
                userService,
                attachmentService,
                storageService,
                permissionService
        );
    }

    @ParameterizedTest
    @CsvSource({
            "Bescheid.pdf, application/pdf",
            "Foto.png, image/png",
            "Foto.jpg, image/jpeg",
            "Animation.gif, image/gif",
            "Notiz.txt, text/plain",
    })
    void previewServesInertMediaTypesInline(String fileName, String expectedMediaType) throws ResponseException {
        storeAttachment(fileName);

        var response = controller.download(jwt, ATTACHMENT_KEY, false);

        assertEquals(MediaType.parseMediaType(expectedMediaType), response.getHeaders().getContentType());
        assertEquals("inline", contentDisposition(response.getHeaders()).getType());
    }

    @ParameterizedTest
    @CsvSource({
            "Rechnung.html",
            "Rechnung.htm",
            "Logo.svg",
            "Daten.xml",
            "Skript.js",
            "Ohne-Endung",
    })
    void previewServesActiveOrUnknownContentAsGenericDownload(String fileName) throws ResponseException {
        storeAttachment(fileName);

        var response = controller.download(jwt, ATTACHMENT_KEY, false);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        var contentDisposition = contentDisposition(response.getHeaders());
        assertEquals("attachment", contentDisposition.getType());
        assertEquals(fileName, contentDisposition.getFilename());
    }

    @Test
    void downloadKeepsInertMediaTypesAsAttachment() throws ResponseException {
        storeAttachment("Bescheid.pdf");

        var response = controller.download(jwt, ATTACHMENT_KEY, true);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("attachment", contentDisposition(response.getHeaders()).getType());
    }

    @Test
    void downloadServesActiveContentWithGenericMediaType() throws ResponseException {
        storeAttachment("Rechnung.html");

        var response = controller.download(jwt, ATTACHMENT_KEY, true);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertEquals("attachment", contentDisposition(response.getHeaders()).getType());
    }

    @Test
    void downloadRequiresReadPermissionForTheAttachmentsProcessInstance() throws ResponseException {
        storeAttachment("Bescheid.pdf");
        doThrow(ResponseException.forbidden()).when(permissionService).requireProcessInstancePermission(
                "user-1",
                42L,
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );

        var exception = assertThrows(ResponseException.class, () -> controller.download(jwt, ATTACHMENT_KEY, false));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(storageService, never()).getDocumentContent(any(), anyString());
    }

    private void storeAttachment(String fileName) throws ResponseException {
        var attachment = new ProcessInstanceAttachmentEntity()
                .setKey(ATTACHMENT_KEY)
                .setFileName(fileName)
                .setProcessInstanceId(42L)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/attachments/file");
        when(attachmentService.retrieve(ATTACHMENT_KEY)).thenReturn(Optional.of(attachment));
    }

    private static ContentDisposition contentDisposition(HttpHeaders headers) {
        return ContentDisposition.parse(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }
}

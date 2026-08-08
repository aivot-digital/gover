package de.aivot.prosuna.backend.mail.controllers;

import de.aivot.prosuna.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.prosuna.backend.mail.dtos.MailConfigurationResponseDTO;
import de.aivot.prosuna.backend.mail.dtos.TestMailRequestDTO;
import de.aivot.prosuna.backend.mail.services.MailConfigurationService;
import de.aivot.prosuna.backend.mail.services.TestMailService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailControllerTest {
    @Mock
    private MailConfigurationService mailConfigurationService;
    @Mock
    private TestMailService testMailService;
    @Mock
    private UserService userService;
    @Mock
    private PermissionService permissionService;

    private MailController mailController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mailController = new MailController(
                mailConfigurationService,
                testMailService,
                userService,
                permissionService
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
    void getConfigurationRequiresSystemConfigUpdatePermission() throws Exception {
        var configuration = new MailConfigurationResponseDTO(
                true,
                "smtp.example.com",
                587,
                true,
                "prosuna",
                true,
                true,
                "Prosuna",
                "service@example.com",
                List.of()
        );
        when(mailConfigurationService.getConfiguration()).thenReturn(configuration);

        var result = mailController.getConfiguration(jwt);

        assertSame(configuration, result);
        verify(permissionService).requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);
    }

    @Test
    void testReturnsSuccessfulContractAfterSending() throws Exception {
        var user = new UserEntity().setId("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));

        var result = mailController.test(jwt, new TestMailRequestDTO("recipient@example.com"));

        assertTrue(result.success());
        verify(permissionService).requireSystemPermission("user-1", ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);
        verify(testMailService).send(user, "recipient@example.com");
    }

    @Test
    void testReturnsErrorContractWhenSendingFails() throws Exception {
        var user = new UserEntity().setId("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        doThrow(new MessagingException("SMTP server unavailable"))
                .when(testMailService)
                .send(user, "recipient@example.com");

        var result = mailController.test(jwt, new TestMailRequestDTO("recipient@example.com"));

        assertFalse(result.success());
        assertEquals("SMTP server unavailable", result.errorMessage());
    }

    @Test
    void testReturnsErrorContractWhenPreparingMailFails() throws Exception {
        var user = new UserEntity().setId("user-1");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Mail template unavailable"))
                .when(testMailService)
                .send(user, "recipient@example.com");

        var result = mailController.test(jwt, new TestMailRequestDTO("recipient@example.com"));

        assertFalse(result.success());
        assertEquals("Mail template unavailable", result.errorMessage());
    }
}

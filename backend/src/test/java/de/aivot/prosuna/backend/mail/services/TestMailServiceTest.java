package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TestMailServiceTest {
    @Mock
    private MailService mailService;
    @Mock
    private MailConfigurationService mailConfigurationService;
    @Mock
    private SystemService systemService;

    private TestMailService testMailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testMailService = new TestMailService(mailService, mailConfigurationService, systemService);
    }

    @Test
    void sendRejectsIncompleteConfigurationInsteadOfReportingSuccess() {
        when(mailConfigurationService.isConfigured()).thenReturn(false);

        var exception = assertThrows(
                MessagingException.class,
                () -> testMailService.send(new UserEntity(), "recipient@example.com")
        );

        assertEquals("Die E-Mail-Anbindung ist nicht vollständig konfiguriert.", exception.getMessage());
        verifyNoInteractions(mailService, systemService);
    }
}

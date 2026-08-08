package de.aivot.prosuna.backend.mail.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailConfigurationServiceTest {
    @Test
    void getConfigurationReturnsExplicitNonSecretConfiguration() throws Exception {
        var mailProperties = new MailProperties();
        mailProperties.setHost(" smtp.example.com ");
        mailProperties.setPort(587);
        mailProperties.setUsername("prosuna@example.com");
        mailProperties.setPassword("top-secret");
        mailProperties.getProperties().put("mail.smtp.auth", "true");
        mailProperties.getProperties().put("mail.smtp.starttls.enable", "true");

        var prosunaConfig = new ProsunaConfig();
        prosunaConfig.setFromMail("\"Prosuna Service\" <service@example.com>");

        var result = new MailConfigurationService(mailProperties, prosunaConfig).getConfiguration();

        assertTrue(result.configured());
        assertEquals("smtp.example.com", result.host());
        assertEquals(587, result.port());
        assertTrue(result.authenticationEnabled());
        assertEquals("prosuna@example.com", result.username());
        assertTrue(result.passwordConfigured());
        assertTrue(result.startTlsEnabled());
        assertEquals("Prosuna Service", result.senderName());
        assertEquals("service@example.com", result.senderAddress());
        assertTrue(result.configurationIssues().isEmpty());

        var serializedResult = new ObjectMapper().writeValueAsString(result);
        assertFalse(serializedResult.contains("top-secret"));
        assertFalse(serializedResult.contains("\"password\":"));
    }

    @Test
    void getConfigurationReportsMissingRequiredValues() {
        var mailProperties = new MailProperties();
        mailProperties.setHost(" ");
        mailProperties.setPort(0);
        mailProperties.getProperties().put("mail.smtp.auth", "true");

        var result = new MailConfigurationService(mailProperties, new ProsunaConfig()).getConfiguration();

        assertFalse(result.configured());
        assertNull(result.host());
        assertFalse(result.passwordConfigured());
        assertTrue(result.configurationIssues().contains("Es ist kein SMTP-Server konfiguriert."));
        assertTrue(result.configurationIssues().contains("Es ist kein gültiger SMTP-Port konfiguriert."));
        assertTrue(result.configurationIssues().contains("Für die SMTP-Authentifizierung ist kein Benutzername konfiguriert."));
        assertTrue(result.configurationIssues().contains("Für die SMTP-Authentifizierung ist kein Kennwort konfiguriert."));
        assertTrue(result.configurationIssues().contains("Es ist keine Absenderadresse konfiguriert."));
    }
}

package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.mail.dtos.MailConfigurationResponseDTO;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MailConfigurationService {
    private static final String AUTH_PROPERTY = "mail.smtp.auth";
    private static final String START_TLS_PROPERTY = "mail.smtp.starttls.enable";

    private final MailProperties mailProperties;
    private final ProsunaConfig prosunaConfig;

    public MailConfigurationService(MailProperties mailProperties, ProsunaConfig prosunaConfig) {
        this.mailProperties = mailProperties;
        this.prosunaConfig = prosunaConfig;
    }

    @Nonnull
    public MailConfigurationResponseDTO getConfiguration() {
        var issues = new ArrayList<String>();
        var host = trimToNull(mailProperties.getHost());
        var port = mailProperties.getPort();
        var username = trimToNull(mailProperties.getUsername());
        var passwordConfigured = StringUtils.isNotNullOrEmpty(mailProperties.getPassword());
        var authenticationEnabled = readBooleanProperty(AUTH_PROPERTY);
        var startTlsEnabled = readBooleanProperty(START_TLS_PROPERTY);

        if (host == null) {
            issues.add("Es ist kein SMTP-Server konfiguriert.");
        }
        if (port == null || port < 1 || port > 65535) {
            issues.add("Es ist kein gültiger SMTP-Port konfiguriert.");
        }
        if (authenticationEnabled && username == null) {
            issues.add("Für die SMTP-Authentifizierung ist kein Benutzername konfiguriert.");
        }
        if (authenticationEnabled && !passwordConfigured) {
            issues.add("Für die SMTP-Authentifizierung ist kein Kennwort konfiguriert.");
        }

        String senderName = null;
        String senderAddress = null;
        var fromMail = trimToNull(prosunaConfig.getFromMail());
        if (fromMail == null) {
            issues.add("Es ist keine Absenderadresse konfiguriert.");
        } else {
            try {
                var sender = new InternetAddress(fromMail, true);
                sender.validate();
                senderName = trimToNull(sender.getPersonal());
                senderAddress = trimToNull(sender.getAddress());
                if (senderAddress == null) {
                    issues.add("Die konfigurierte Absenderadresse ist ungültig.");
                }
            } catch (AddressException e) {
                issues.add("Die konfigurierte Absenderadresse ist ungültig.");
            }
        }

        return new MailConfigurationResponseDTO(
                issues.isEmpty(),
                host,
                port,
                authenticationEnabled,
                username,
                passwordConfigured,
                startTlsEnabled,
                senderName,
                senderAddress,
                List.copyOf(issues)
        );
    }

    public boolean isConfigured() {
        return getConfiguration().configured();
    }

    private boolean readBooleanProperty(String key) {
        return Boolean.parseBoolean(mailProperties.getProperties().getOrDefault(key, "false").trim());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

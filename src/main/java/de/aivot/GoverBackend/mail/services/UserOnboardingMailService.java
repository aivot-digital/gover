package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.dtos.UserInitialCredentialsDTO;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

@Component
public class UserOnboardingMailService {
    private final MailService mailService;
    private final SystemService systemService;

    @Autowired
    public UserOnboardingMailService(
            MailService mailService,
            SystemService systemService
    ) {
        this.mailService = mailService;
        this.systemService = systemService;
    }

    public boolean isSendingConfigured() {
        return mailService.isSendingConfigured();
    }

    public void send(UserEntity createdUser, UserInitialCredentialsDTO initialCredentials) throws MessagingException, IOException, MailException, ResponseException {
        var context = new HashMap<String, Object>();
        context.put("title", "Ihre Zugangsdaten für Gover");
        context.put("user", createdUser);
        context.put("initialCredentials", initialCredentials);
        context.put("loginUrl", "/staff");

        var theme = systemService
                .retrieveDefaultTheme();

        mailService.sendMail(
                theme,
                createdUser.getEmail(),
                Optional.empty(),
                Optional.empty(),
                "[Gover] Ihre Zugangsdaten",
                MailTemplate.StaffAccountCredentials,
                context,
                Optional.empty()
        );
    }
}

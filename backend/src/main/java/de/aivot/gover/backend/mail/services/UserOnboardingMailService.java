package de.aivot.gover.backend.mail.services;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.enums.MailTemplate;
import de.aivot.gover.backend.system.services.SystemService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.dtos.UserInitialCredentialsDTO;
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

package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

@Component
public class TestMailService {
    private final MailService mailService;
    private final SystemService systemService;

    @Autowired
    public TestMailService(MailService mailService, SystemService systemService) {
        this.mailService = mailService;
        this.systemService = systemService;
    }

    public void send(UserEntity triggeringUser, String to) throws MessagingException, IOException, ResponseException {
        String title = "SMTP-Test";

        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("triggeringUser", triggeringUser);

        var theme = systemService
                .retrieveDefaultTheme();

        mailService.sendMail(
                theme,
                to,
                Optional.empty(),
                Optional.empty(),
                "[Gover] [Test] " + title,
                MailTemplate.SmtpTest,
                context,
                Optional.empty()
        );
    }
}
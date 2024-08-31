package de.aivot.GoverBackend.services.mail;

import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

@Component
public class SmtpTestMailService {
    private final MailService mailService;

    @Autowired
    public SmtpTestMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void send(KeyCloakDetailsUser triggeringUser, String to) throws MessagingException, IOException {
        String title = "SMTP-Test";

        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("triggeringUser", triggeringUser);

        mailService.sendMail(
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
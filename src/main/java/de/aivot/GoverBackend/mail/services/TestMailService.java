package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
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

    @Autowired
    public TestMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void send(UserEntity triggeringUser, String to) throws MessagingException, IOException, ResponseException {
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
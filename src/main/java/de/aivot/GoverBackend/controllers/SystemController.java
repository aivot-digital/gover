package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.system.TestSmtpPayload;
import de.aivot.GoverBackend.models.system.TestSmtpResult;
import de.aivot.GoverBackend.services.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.io.IOException;

@RestController
@CrossOrigin
public class SystemController {
    private final MailService mailService;

    @Autowired
    public SystemController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/api/system/test-smtp")
    public TestSmtpResult testSmtp(@RequestBody TestSmtpPayload payload) {
        var result = new TestSmtpResult();

        try {
            mailService.sendMail(
                    payload.getTargetMail(),
                    "[Gover] SMTP-Test",
                    "Der Test der SMTP-Verbindung war erfolgreich!",
                    "<h1>Der Test der SMTP-Verbindung war erfolgreich!</h1>"
            );
        } catch (MessagingException | IOException e) {
            result.setResult(e.getMessage());
        }

        return result;
    }
}

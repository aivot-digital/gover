package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.MailTemplate;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.SmtpResultDto;
import de.aivot.GoverBackend.models.dtos.TestSmtpDto;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.services.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
public class SystemController {
    private final MailService mailService;
    private final GoverConfig goverConfig;

    @Autowired
    public SystemController(
            MailService mailService,
            GoverConfig goverConfig
    ) {
        this.mailService = mailService;
        this.goverConfig = goverConfig;
    }

    @IsAdmin
    @PostMapping("/api/system/test-smtp")
    public SmtpResultDto testSmtp(
            Authentication authentication,
            @RequestBody TestSmtpDto payload
    ) {
        var result = new SmtpResultDto();

        try {
            mailService.sendMail(
                    payload.getTargetMail(),
                    "[Gover] SMTP-Test",
                    MailTemplate.SmtpTestMail,
                    new HashMap<>()
            );
        } catch (MessagingException | IOException e) {
            result.setResult(e.getMessage());
        }

        return result;
    }

    @GetMapping("/api/public/sentry-dns")
    public List<String> getSentryDns() {
        return List.of(goverConfig.getSentryWebApp());
    }

    @GetMapping("/api/public/environment")
    public List<String> getEnvironment() {
        return List.of(goverConfig.getEnvironment());
    }

    @GetMapping("/api/system/file-extensions")
    public List<String> getFileExtensions() {
        return goverConfig.getFileExtensions();
    }

    @GetMapping("/api/system/content-types")
    public List<String> getContentTypes() {
        return goverConfig.getContentTypes();
    }
}

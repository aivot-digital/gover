package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.mail.SmtpTestMailService;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.SmtpResultDto;
import de.aivot.GoverBackend.models.dtos.TestSmtpDto;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class SystemController {
    private final SmtpTestMailService mailService;
    private final GoverConfig goverConfig;

    @Autowired
    public SystemController(
            SmtpTestMailService mailService,
            GoverConfig goverConfig
    ) {
        this.mailService = mailService;
        this.goverConfig = goverConfig;
    }

    @PostMapping("/api/system/test-smtp")
    public SmtpResultDto testSmtp(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody TestSmtpDto payload
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);

        user.ifNotAdminThrow(ForbiddenException::new);

        var result = new SmtpResultDto();

        try {
            mailService.send(user, payload.getTargetMail());
        } catch (MessagingException | IOException e) {
            result.setResult(e.getMessage());
        }

        return result;
    }

    @GetMapping("/api/public/system/sentry-dsn")
    public List<String> getSentryDns() {
        return List.of(goverConfig.getSentryWebApp());
    }

    @GetMapping("/api/system/file-extensions")
    public List<String> getFileExtensions() {
        return goverConfig.getFileExtensions();
    }
}

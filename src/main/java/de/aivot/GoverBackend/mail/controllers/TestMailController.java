package de.aivot.GoverBackend.mail.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.dtos.TestMailRequestDTO;
import de.aivot.GoverBackend.mail.dtos.TestMailResponseDTO;
import de.aivot.GoverBackend.mail.services.TestMailService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

@RestController
@RequestMapping("/api/mail/test/")
public class TestMailController {
    private final TestMailService testMailService;

    @Autowired
    public TestMailController(
            TestMailService testMailService
    ) {
        this.testMailService = testMailService;
    }

    @PostMapping("")
    public TestMailResponseDTO test(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody TestMailRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        try {
            testMailService
                    .send(user, requestDTO.targetMail());
        } catch (MessagingException | IOException e) {
            return TestMailResponseDTO.createError(e);
        }

        return TestMailResponseDTO.createSuccess();
    }
}

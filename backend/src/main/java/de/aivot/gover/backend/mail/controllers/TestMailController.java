package de.aivot.gover.backend.mail.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.dtos.TestMailRequestDTO;
import de.aivot.gover.backend.mail.dtos.TestMailResponseDTO;
import de.aivot.gover.backend.mail.services.TestMailService;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

@RestController
@RequestMapping("/api/mail/test/")
@Tag(
        name = "Mail",
        description = "Endpoints for testing mail functionality"
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class TestMailController {
    private final TestMailService testMailService;
    private final UserService userService;

    @Autowired
    public TestMailController(
            TestMailService testMailService,
            UserService userService) {
        this.testMailService = testMailService;
        this.userService = userService;
    }

    @PostMapping("")
    @Operation(
            summary = "Send Test Mail",
            description = "Send a test mail to the specified email address."
    )
    public TestMailResponseDTO test(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody TestMailRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
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

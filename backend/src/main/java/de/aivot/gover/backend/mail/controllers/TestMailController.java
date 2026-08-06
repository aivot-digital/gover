package de.aivot.gover.backend.mail.controllers;

import de.aivot.gover.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.dtos.TestMailRequestDTO;
import de.aivot.gover.backend.mail.dtos.TestMailResponseDTO;
import de.aivot.gover.backend.mail.services.TestMailService;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
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
    private final PermissionService permissionService;

    @Autowired
    public TestMailController(
            TestMailService testMailService,
            UserService userService,
            PermissionService permissionService) {
        this.testMailService = testMailService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @PostMapping("")
    @Operation(
            summary = "Send Test Mail",
            description = "Send a test mail to the specified email address. Requires the system-level permission `" +
                    ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE + "`."
    )
    public TestMailResponseDTO test(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody TestMailRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);

        try {
            testMailService
                    .send(user, requestDTO.targetMail());
        } catch (MessagingException | IOException e) {
            return TestMailResponseDTO.createError(e);
        }

        return TestMailResponseDTO.createSuccess();
    }
}

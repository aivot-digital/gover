package de.aivot.prosuna.backend.mail.controllers;

import de.aivot.prosuna.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.dtos.MailConfigurationResponseDTO;
import de.aivot.prosuna.backend.mail.dtos.TestMailRequestDTO;
import de.aivot.prosuna.backend.mail.dtos.TestMailResponseDTO;
import de.aivot.prosuna.backend.mail.services.MailConfigurationService;
import de.aivot.prosuna.backend.mail.services.TestMailService;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail/")
@Tag(
        name = "Mail",
        description = "Endpoints for inspecting and testing mail functionality"
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class MailController {
    private final MailConfigurationService mailConfigurationService;
    private final TestMailService testMailService;
    private final UserService userService;
    private final PermissionService permissionService;

    public MailController(
            MailConfigurationService mailConfigurationService,
            TestMailService testMailService,
            UserService userService,
            PermissionService permissionService
    ) {
        this.mailConfigurationService = mailConfigurationService;
        this.testMailService = testMailService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("configuration/")
    @Operation(
            summary = "Retrieve Mail Configuration",
            description = "Retrieve non-secret details about the configured mail service. Requires the system-level permission `" +
                    ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE + "`."
    )
    public MailConfigurationResponseDTO getConfiguration(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        permissionService.requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);
        return mailConfigurationService.getConfiguration();
    }

    @PostMapping("test/")
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

        permissionService.requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);

        try {
            testMailService.send(user, requestDTO.targetMail());
        } catch (Exception e) {
            // The diagnostic endpoint reports failures from the complete delivery pipeline, including template rendering.
            return TestMailResponseDTO.createError(e);
        }

        return TestMailResponseDTO.createSuccess();
    }
}

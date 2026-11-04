package de.aivot.prosuna.backend.nocode.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunRequestDTO;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
import de.aivot.prosuna.backend.nocode.services.NoCodeDryRunService;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "No-Code Operators",
        description = "No-Code operators are the building blocks for creating no-code functionalities."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class NoCodeDryRunController {
    private final NoCodeDryRunService noCodeDryRunService;

    public NoCodeDryRunController(@Nonnull NoCodeDryRunService noCodeDryRunService) {
        this.noCodeDryRunService = noCodeDryRunService;
    }

    @PostMapping("/api/no-code/dry-run/")
    @Operation(
            summary = "Dry Run No-Code Expression",
            description = "Evaluate a no-code expression against test data and return the result of every operand. " +
                    "Nothing is saved, and operators that read protected data or cause side effects are skipped. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE + "` in at least one process."
    )
    public NoCodeDryRunResultDTO dryRun(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody NoCodeDryRunRequestDTO request
    ) throws ResponseException {
        return noCodeDryRunService.dryRun(UserService.getIdFromJWT(jwt), request);
    }
}

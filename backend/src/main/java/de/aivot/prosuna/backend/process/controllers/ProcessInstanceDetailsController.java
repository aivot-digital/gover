package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceDetailsDTO;
import de.aivot.prosuna.backend.process.services.ProcessInstanceDetailsService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process-instances/")
@Tag(name = "Process instance details")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceDetailsController {
    private final UserService users;
    private final ProcessInstanceDetailsService details;

    public ProcessInstanceDetailsController(UserService users, ProcessInstanceDetailsService details) {
        this.users = users;
        this.details = details;
    }

    @Nonnull
    @GetMapping("{id}/details/")
    @Operation(summary = "Retrieve process instance details", description = "Returns instance metadata and active task labels. Requires process_instance.read on this instance.")
    public ProcessInstanceDetailsDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                              @Nonnull @PathVariable Long id) throws ResponseException {
        var user = users.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return details.retrieve(user.getId(), id);
    }
}

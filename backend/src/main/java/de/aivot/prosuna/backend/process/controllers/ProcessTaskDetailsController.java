package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.process.dtos.ProcessTaskDetailsDTO;
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
@RequestMapping("/api/process-instance-tasks/")
@Tag(name = "Process task details")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessTaskDetailsController {
    private final UserService users;
    private final ProcessInstanceDetailsService details;

    public ProcessTaskDetailsController(@Nonnull UserService users, @Nonnull ProcessInstanceDetailsService details) {
        this.users = users;
        this.details = details;
    }

    @Nonnull
    @GetMapping("{id}/details/")
    @Operation(summary = "Retrieve process task details", description = "Returns task and instance data with display metadata. Requires process_instance.read on the owning instance or at system level.")
    public ProcessTaskDetailsDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                          @Nonnull @PathVariable Long id) throws ResponseException {
        var user = users.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return details.retrieveTask(user.getId(), id);
    }
}

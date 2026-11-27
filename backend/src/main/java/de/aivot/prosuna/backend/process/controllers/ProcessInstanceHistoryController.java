package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceHistoryTasksDTO;
import de.aivot.prosuna.backend.process.services.ProcessInstanceHistoryService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process-instances/{instanceId}/history/")
@Tag(name = OpenApiConstants.Tags.ProcessesDefinitionsName)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceHistoryController {
    private final UserService users;
    private final ProcessInstanceHistoryService history;

    public ProcessInstanceHistoryController(@Nonnull UserService users, @Nonnull ProcessInstanceHistoryService history) {
        this.users = users;
        this.history = history;
    }

    @GetMapping
    @Operation(summary = "Retrieve a page of process instance history tasks",
            description = "Returns tasks ordered by start time and ID, excluding replaced restart attempts. Includes referenced predecessors for data comparisons and active task metadata for the preview. taskId selects the containing page and accepts an ID, active, current, failed or latest. Requires process_instance.read for the instance. Maximum page size: 100.")
    public ProcessInstanceHistoryTasksDTO getTasks(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable long instanceId,
                                                   @Nonnull @ParameterObject @PageableDefault(size = 20) Pageable pageable,
                                                   @Nullable @RequestParam(required = false) String taskId) throws ResponseException {
        var user = users.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return history.getTasks(user.getId(), instanceId, pageable, taskId);
    }
}

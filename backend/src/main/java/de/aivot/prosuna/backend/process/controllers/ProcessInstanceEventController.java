package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceEventFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceEventService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process-instance-events/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance history events."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceEventController {
    // Process-instance events form an append-only history. They are written by backend loggers and therefore expose
    // only read endpoints here; clients cannot create, alter, or delete execution and audit history.
    private final UserService userService;
    private final ProcessInstanceEventService processInstanceHistoryEventService;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceEventController(UserService userService,
                                          ProcessInstanceEventService processInstanceHistoryEventService,
                                          PermissionService permissionService) {
        this.userService = userService;
        this.processInstanceHistoryEventService = processInstanceHistoryEventService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance History Events",
            description = "List all process instance history events with optional filtering and pagination."
    )
    public Page<ProcessInstanceEventEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceEventFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)) {
            if (filter.getProcessInstanceId() != null) {
                permissionService.requireProcessInstancePermission(
                        user.getId(),
                        filter.getProcessInstanceId(),
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
                );
            } else {
                var accessibleProcessInstanceIds = permissionService
                        .getProcessInstancesWithPermission(user.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);

                if (filter.getProcessInstanceIds() != null) {
                    accessibleProcessInstanceIds = filter.getProcessInstanceIds()
                            .stream()
                            .filter(accessibleProcessInstanceIds::contains)
                            .toList();
                }

                if (accessibleProcessInstanceIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setProcessInstanceIds(accessibleProcessInstanceIds);
            }
        }

        return processInstanceHistoryEventService
                .list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance History Event",
            description = "Retrieve a process instance history event by its ID."
    )
    public ProcessInstanceEventEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var event = processInstanceHistoryEventService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                event.getProcessInstanceId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );

        return event;
    }

}

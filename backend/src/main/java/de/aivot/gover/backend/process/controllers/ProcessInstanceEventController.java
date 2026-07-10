package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.gover.backend.process.filters.ProcessInstanceEventFilter;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.ProcessInstanceEventService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
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

import java.util.Map;

@RestController
@RequestMapping("/api/process-instance-events/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance history events."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceEventController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceEventService processInstanceHistoryEventService;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceEventController(AuditService auditService,
                                          UserService userService,
                                          ProcessInstanceEventService processInstanceHistoryEventService,
                                          PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceEventController.class, "Prozesse");
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

        if (!permissionService.checkSystemPermission(user.getId(), ProcessPermissionProvider.PROCESS_INSTANCE_READ)) {
            if (filter.getProcessInstanceId() != null) {
                permissionService.hasProcessInstancePermission(
                        user.getId(),
                        filter.getProcessInstanceId(),
                        ProcessPermissionProvider.PROCESS_INSTANCE_READ
                );
            } else {
                var accessibleProcessInstanceIds = permissionService
                        .getProcessInstancesWithPermission(user.getId(), ProcessPermissionProvider.PROCESS_INSTANCE_READ);

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

    @PostMapping("")
    @Operation(
            summary = "Create Process Instance History Event",
            description = "Create a new process instance history event. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessInstanceEventEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessInstanceEventEntity newEvent
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.hasProcessInstancePermission(
                execUser.getId(),
                newEvent.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );

        var result = processInstanceHistoryEventService
                .create(newEvent);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, ProcessInstanceEventEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        )).withMessage(
                "Das Instanzereignis mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
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

        permissionService.hasProcessInstancePermission(
                user.getId(),
                event.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_READ
        );

        return event;
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Instance History Event",
            description = "Update an existing process instance history event. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceEventEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceEventEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceHistoryEventService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.hasProcessInstancePermission(
                execUser.getId(),
                existing.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );

        updateDTO.setId(existing.getId());

        var result = processInstanceHistoryEventService
                .update(id, updateDTO);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, ProcessInstanceEventEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        )).withMessage(
                "Das Instanzereignis mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Instance History Event",
            description = "Delete a process instance history event by its ID. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceHistoryEventService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.hasProcessInstancePermission(
                user.getId(),
                existing.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );

        var deleted = processInstanceHistoryEventService
                .delete(id);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, ProcessInstanceEventEntity.class, deleted.getId(), "id", Map.of(
                "id", deleted.getId(),
                "processInstanceId", deleted.getProcessInstanceId(),
                "processInstanceTaskId", deleted.getProcessInstanceTaskId()
        )).withMessage(
                "Das Instanzereignis mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(deleted.getId())),
                StringUtils.quote(String.valueOf(deleted.getProcessInstanceId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}

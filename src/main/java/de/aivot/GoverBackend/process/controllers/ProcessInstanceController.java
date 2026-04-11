package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.filters.ProcessInstanceFilter;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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
@RequestMapping("/api/process-instances/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instances."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceService processInstanceService;
    private final DepartmentService departmentService;
    private final ProcessService processDefinitionService;

    @Autowired
    public ProcessInstanceController(AuditService auditService,
                                    UserService userService,
                                    ProcessInstanceService processInstanceService,
                                    DepartmentService departmentService,
                                    ProcessService processDefinitionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceController.class, "Prozesse");
        this.userService = userService;
        this.processInstanceService = processInstanceService;
        this.departmentService = departmentService;
        this.processDefinitionService = processDefinitionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instances",
            description = "List all process instances with optional filtering and pagination."
    )
    public Page<ProcessInstanceEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceFilter filter
    ) throws ResponseException {
        return processInstanceService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Instance",
            description = "Create a new process instance. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessInstanceEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessInstanceEntity newInstance
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = processInstanceService
                .create(newInstance);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, ProcessInstanceEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance",
            description = "Retrieve a process instance by its ID."
    )
    public ProcessInstanceEntity retrieve(
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        return processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Instance",
            description = "Update an existing process instance. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        updateDTO.setId(existing.getId());

        var result = processInstanceService
                .update(id, updateDTO);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, ProcessInstanceEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Instance",
            description = "Delete a process instance by its ID. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deleted = processInstanceService
                .delete(id);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, ProcessInstanceEntity.class, deleted.getId(), "id", Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(deleted.getId())),
                StringUtils.quote(String.valueOf(deleted.getProcessId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}

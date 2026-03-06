package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.GoverBackend.process.filters.ProcessInstanceAccessControlFilter;
import de.aivot.GoverBackend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.GoverBackend.process.services.PotentialProcessInstanceAccessService;
import de.aivot.GoverBackend.process.services.ProcessInstanceAccessControlService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/process-instance-access-controls/")
@Tag(
        name = OpenApiConstants.Tags.ProcessAccessControlsName,
        description = OpenApiConstants.Tags.ProcessAccessControlsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAccessControlController extends GenericCrudController<ProcessInstanceAccessControlEntity, Integer, ProcessInstanceAccessControlFilter> {
    private final UserService userService;
    private final PotentialProcessInstanceAccessService potentialProcessInstanceAccessService;

    public ProcessInstanceAccessControlController(AuditService auditService,
                                                  UserService userService,
                                                  ProcessInstanceAccessControlService processInstanceAccessControlService,
                                                  PotentialProcessInstanceAccessService potentialProcessInstanceAccessService) {
        super(auditService.createScopedAuditService(ProcessInstanceAccessControlController.class),
                userService,
                processInstanceAccessControlService);
        this.userService = userService;
        this.potentialProcessInstanceAccessService = potentialProcessInstanceAccessService;
    }

    @GetMapping("potential-options/")
    @Operation(
            summary = "List potential selectable assignees for a process instance",
            description = "Returns users, organisation units and teams that can be selected based on process access and optional required permissions."
    )
    public List<ProcessInstanceAccessSelectableItem> listPotentialOptions(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam Integer processId,
            @Nonnull @RequestParam Integer processVersion,
            @RequestParam(required = false) List<String> requiredPermissions
    ) throws ResponseException {
        userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return potentialProcessInstanceAccessService.listSelectableItems(
                processId,
                processVersion,
                requiredPermissions
        );
    }

    // TODO: Implement Permission Checks
}

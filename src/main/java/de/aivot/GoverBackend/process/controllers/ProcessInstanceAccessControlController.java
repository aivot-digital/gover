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
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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
        super(auditService.createScopedAuditService(ProcessInstanceAccessControlController.class, "Prozesse"),
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

    @Override
    protected Integer getIdForEntity(ProcessInstanceAccessControlEntity entity) {
        return entity.getId();
    }

    @Override
    @Nonnull
    protected String buildCreateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull ProcessInstanceAccessControlEntity createdItem) {
        return String.format(
                "Die Instanz-Zugriffsregel mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(createdItem.getId())),
                StringUtils.quote(String.valueOf(createdItem.getTargetProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildUpdateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessInstanceAccessControlEntity updatedItem) {
        return String.format(
                "Die Instanz-Zugriffsregel mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(updatedItem.getTargetProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildDeleteAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessInstanceAccessControlEntity deletedItem) {
        return String.format(
                "Die Instanz-Zugriffsregel mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(deletedItem.getTargetProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    // TODO: Implement Permission Checks
}

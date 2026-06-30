package de.aivot.gover.backend.form.controllers;

import de.aivot.gover.backend.enums.EntityLockState;
import de.aivot.gover.backend.form.entities.VFormWithPermissionsEntity;
import de.aivot.gover.backend.form.services.FormLockService;
import de.aivot.gover.backend.form.services.VFormWithPermissionsService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.models.dtos.EntityLockDto;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms/{formId}/lock/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class FormLockController {
    private final FormLockService formLockService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final UserService userService;

    @Autowired
    public FormLockController(
            FormLockService formLockService,
            VFormWithPermissionsService vFormWithPermissionsService, UserService userService) {
        this.formLockService = formLockService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve form lock",
            description = "Retrieve the lock status of a form. " +
                          "Indicates whether the form is locked, and if so, by which user."
    )
    public EntityLockDto retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    execUser.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead);
        }

        return formLockService
                .retrieve(formId)
                .map(
                        lock -> new EntityLockDto(
                                execUser.hasId(lock.getUserId()) ? EntityLockState.LockedSelf : EntityLockState.LockedOther,
                                lock.getUserId()
                        )
                )
                .orElse(new EntityLockDto(EntityLockState.Free, null));
    }

    @DeleteMapping("")
    @Operation(
            summary = "Delete form lock",
            description = "Delete the lock on a form. " +
                          "Only the user who created the lock can delete it."
    )
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var lock = formLockService
                .retrieve(formId);

        if (lock.isPresent()) {
            var lockedByUserId = lock.get().getUserId();

            if (execUser.hasId(lockedByUserId)) {
                formLockService.delete(lock.get().getFormId());
            } else {
                throw ResponseException.conflict("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}

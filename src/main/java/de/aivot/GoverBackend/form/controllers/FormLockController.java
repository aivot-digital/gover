package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.enums.EntityLockState;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.dtos.EntityLockDto;
import de.aivot.GoverBackend.security.OpenAPISecurityConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms/{formId}/lock/")
@Tag(name = "Form", description = "Interact with forms")
public class FormLockController {
    private final FormLockService formLockService;
    private final VFormWithPermissionsService vFormWithPermissionsService;

    @Autowired
    public FormLockController(
            FormLockService formLockService,
            VFormWithPermissionsService vFormWithPermissionsService) {
        this.formLockService = formLockService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
    }

    @GetMapping("")
    @Operation(summary = "Retrieve form lock", description = "Retrieve the lock status of a form.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public EntityLockDto retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        vFormWithPermissionsService.checkUserPermission(
                formId,
                user.getId(),
                VFormWithPermissionsEntity::getFormPermissionRead,
                PermissionLabels.FormPermissionRead);

        return formLockService
                .retrieve(formId)
                .map(
                        lock -> new EntityLockDto(
                                user.hasId(lock.getUserId()) ? EntityLockState.LockedSelf : EntityLockState.LockedOther,
                                lock.getUserId()
                        )
                )
                .orElse(new EntityLockDto(EntityLockState.Free, null));
    }

    @DeleteMapping("")
    @Operation(summary = "Delete form lock", description = "Delete the lock on a form.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var lock = formLockService
                .retrieve(formId);

        if (lock.isPresent()) {
            var lockedByUserId = lock.get().getUserId();

            if (user.hasId(lockedByUserId)) {
                formLockService.delete(lock.get().getFormId());
            } else {
                throw ResponseException.conflict("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}

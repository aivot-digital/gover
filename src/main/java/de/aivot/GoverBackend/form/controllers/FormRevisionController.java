package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.services.OrganizationalUnitMembershipService;
import de.aivot.GoverBackend.form.cache.entities.FormLockCacheEntity;
import de.aivot.GoverBackend.form.dtos.FormDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormRevisionResponseDTO;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/forms/{formId}/{formVersion}/revisions/")
public class FormRevisionController {
    private final ScopedAuditService auditService;

    private final FormService formService;
    private final FormLockService formLockService;
    private final OrganizationalUnitMembershipService organizationalUnitMembershipService;
    private final FormRevisionService formRevisionService;
    private final FormVersionWithDetailsService formVersionWithDetailsService;

    @Autowired
    public FormRevisionController(
            AuditService auditService,
            FormService formService,
            OrganizationalUnitMembershipService organizationalUnitMembershipService,
            FormLockService formLockService,
            FormRevisionService formRevisionService,
            FormVersionWithDetailsService formVersionWithDetailsService) {
        this.auditService = auditService.createScopedAuditService(FormRevisionController.class);
        this.formService = formService;
        this.formLockService = formLockService;
        this.organizationalUnitMembershipService = organizationalUnitMembershipService;
        this.formRevisionService = formRevisionService;
        this.formVersionWithDetailsService = formVersionWithDetailsService;
    }

    @GetMapping("")
    public Page<FormRevisionResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        if (!user.getGlobalAdmin() && organizationalUnitMembershipService.checkUserNotInDepartment(user, form.getDevelopingOrganizationalUnitId())) {
            throw ResponseException.forbidden("Nur globale Administrator:innen oder Mitarbeiter:innen des Fachbereich können die Formular-Historie ansehen.");
        }

        return formRevisionService
                .list(formId, formVersion, pageable)
                .map(FormRevisionResponseDTO::fromEntity);
    }

    @GetMapping("rollback/{revisionId}/")
    public FormDetailsResponseDTO rollback(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId,
            @PathVariable Integer formVersion,
            @PathVariable BigInteger revisionId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formVersionWithDetailsService
                .retrieve(formId, formVersion)
                .orElseThrow(ResponseException::notFound);

        if (!user.getGlobalAdmin() && organizationalUnitMembershipService.checkUserNotInDepartment(user, form.getDevelopingDepartmentId())) {
            throw ResponseException.forbidden("Nur globale Administrator:innen oder Mitarbeiter:innen des Fachbereich können Formulare zurücksetzen.");
        }

        var existingFormLock = formLockService
                .retrieve(formId)
                .orElse(null);

        if (existingFormLock != null) {
            if (!existingFormLock.getUserId().equals(user.getId())) {
                throw new ResponseException(HttpStatus.LOCKED, "Das Formular ist aktuell durch eine andere Mitarbeiter:in gesperrt. Bitte versuchen Sie es später erneut.");
            }
        } else {
            var formLock = new FormLockCacheEntity()
                    .setFormId(formId)
                    .setUserId(user.getId());

            formLockService.create(formLock);
        }

        var rolledBackForm = formRevisionService.rollback(form, revisionId);

        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", rolledBackForm.getId(),
                "formSlug", rolledBackForm.getSlug(),
                "developingDepartmentId", rolledBackForm.getDevelopingDepartmentId()
        ));
        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", rolledBackForm.getFormId(),
                "formVersion", rolledBackForm.getVersion()
        ));

        // Create a revision for the form
        formRevisionService.create(user, rolledBackForm, form);

        return FormDetailsResponseDTO
                .fromEntity(rolledBackForm);
    }
}

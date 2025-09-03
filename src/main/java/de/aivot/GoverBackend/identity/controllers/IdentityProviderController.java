package de.aivot.GoverBackend.identity.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.filters.FormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionWithDetailsRepository;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderDetailsDTO;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderListDTO;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderPrepareDTO;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderRequestDTO;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.filters.IdentityProviderFilter;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity-providers/")
public class IdentityProviderController {
    private final ScopedAuditService auditService;

    private final IdentityProviderService identityProviderService;
    private final FormRevisionService formRevisionService;
    private final FormRepository formRepository;
    private final FormVersionWithDetailsRepository formVersionWithDetailsRepository;
    private final FormVersionRepository formVersionRepository;

    @Autowired
    public IdentityProviderController(
            AuditService auditService,
            IdentityProviderService identityProviderService,
            FormRevisionService formRevisionService,
            FormRepository formRepository,
            FormVersionWithDetailsRepository formVersionWithDetailsRepository, FormVersionRepository formVersionRepository) {
        this.auditService = auditService
                .createScopedAuditService(IdentityProviderController.class);

        this.identityProviderService = identityProviderService;
        this.formRevisionService = formRevisionService;
        this.formRepository = formRepository;
        this.formVersionWithDetailsRepository = formVersionWithDetailsRepository;
        this.formVersionRepository = formVersionRepository;
    }

    @GetMapping("")
    public Page<IdentityProviderListDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid IdentityProviderFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return identityProviderService
                .list(pageable, filter)
                .map(IdentityProviderListDTO::from);
    }

    @PostMapping("prepare/")
    public IdentityProviderDetailsDTO prepare(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody IdentityProviderPrepareDTO requestDTO
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var preparedEntity = identityProviderService
                .prepare(requestDTO.endpoint());

        return IdentityProviderDetailsDTO
                .from(preparedEntity);
    }

    @PostMapping("")
    public IdentityProviderDetailsDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody IdentityProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var created = identityProviderService
                .create(requestDTO.toEntity());

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        IdentityProviderEntity.class,
                        Map.of(
                                "key", created.getKey(),
                                "name", created.getName()
                        )
                );

        return IdentityProviderDetailsDTO
                .from(created);
    }

    @GetMapping("{key}/")
    public IdentityProviderDetailsDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return identityProviderService
                .retrieve(key)
                .map(IdentityProviderDetailsDTO::from)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    public IdentityProviderDetailsDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid IdentityProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var formFilter = FormVersionWithDetailsFilter
                .create()
                .setIdentityProviderKey(key)
                .setStatus(FormStatus.Published);

        if (!requestDTO.isEnabled() && formVersionWithDetailsRepository.exists(formFilter.build())) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s kann nicht deaktiviert werden, da veröffentlichte Formulare existieren, die diesen Anbieter verwenden.",
                    key
            );
        }

        var updatedEntity = identityProviderService
                .update(key, requestDTO.toEntity());

        if (!updatedEntity.getIsEnabled()) {
            var linkedFormFilter = FormVersionWithDetailsFilter
                    .create()
                    .setIdentityProviderKey(key);

            var linkedForms = formVersionWithDetailsRepository
                    .findAll(linkedFormFilter.build());

            for (var form : linkedForms) {
                var formClone = form
                        .clone();

                var identityProvidersWithoutThisIdentityProvider = form.getIdentityProviders()
                        .stream()
                        .filter(link -> link.getIdentityProviderKey() != null && !link.getIdentityProviderKey().equals(key))
                        .toList();

                form.setIdentityProviders(identityProvidersWithoutThisIdentityProvider);
                formVersionRepository.save(form.toVersionEntity());

                formRevisionService
                        .create(user, form, formClone);
            }
        }

        auditService.logAction(user, AuditAction.Update, IdentityProviderEntity.class, Map.of(
                "key", updatedEntity.getKey(),
                "name", updatedEntity.getName()
        ));

        return IdentityProviderDetailsDTO
                .from(updatedEntity);
    }

    @DeleteMapping("{key}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = identityProviderService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        if (entity.getIsEnabled()) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s kann nicht gelöscht werden, da er aktiviert ist.",
                    key
            );
        }

        var formFilter = new FormVersionWithDetailsFilter()
                .setIdentityProviderKey(key)
                .setStatus(FormStatus.Published);

        if (formVersionWithDetailsRepository.exists(formFilter.build())) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s kann nicht gelöscht werden, da veröffentlichte Formulare existieren, die diesen Anbieter verwenden.",
                    key
            );
        }

        var linkedFormFilter = new FormVersionWithDetailsFilter()
                .setIdentityProviderKey(key);

        var linkedForms = formVersionWithDetailsRepository
                .findAll(linkedFormFilter.build());

        for (var form : linkedForms) {
            var formClone = form
                    .clone();

            var identityProvidersWithoutThisIdentityProvider = form.getIdentityProviders()
                    .stream()
                    .filter(link -> link.getIdentityProviderKey() != null && !link.getIdentityProviderKey().equals(key))
                    .toList();

            form.setIdentityProviders(identityProvidersWithoutThisIdentityProvider);
            formVersionRepository.save(form.toVersionEntity());

            formRevisionService
                    .create(user, form, formClone);
        }


        var deletedEntity = identityProviderService
                .delete(key);

        auditService.logAction(user, AuditAction.Delete, IdentityProviderEntity.class, Map.of(
                "key", deletedEntity.getKey(),
                "name", deletedEntity.getName()
        ));
    }
}

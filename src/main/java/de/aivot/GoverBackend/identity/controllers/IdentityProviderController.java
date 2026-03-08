package de.aivot.GoverBackend.identity.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.form.repositories.VFormVersionWithDetailsRepository;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderDetailsDTO;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderListDTO;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderPrepareDTO;
import de.aivot.GoverBackend.identity.dtos.IdentityProviderRequestDTO;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.filters.IdentityProviderFilter;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity-providers/")
@Tag(
        name = "Identity Providers",
        description = "Identity providers are used to authenticate citizens in the application. " +
                      "They can be configured by systems administrators and linked to forms to enable user authentication. " +
                      "Identity providers support OAuth2 and OpenID Connect protocols and provide mappings for user attributes."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class IdentityProviderController {
    private final ScopedAuditService auditService;

    private final IdentityProviderService identityProviderService;
    private final FormRevisionService formRevisionService;
    private final VFormVersionWithDetailsRepository formVersionWithDetailsRepository;
    private final FormVersionRepository formVersionRepository;
    private final UserService userService;

    @Autowired
    public IdentityProviderController(AuditService auditService,
                                      IdentityProviderService identityProviderService,
                                      FormRevisionService formRevisionService,
                                      VFormVersionWithDetailsRepository formVersionWithDetailsRepository,
                                      FormVersionRepository formVersionRepository,
                                      UserService userService) {
        this.auditService = auditService
                .createScopedAuditService(IdentityProviderController.class);

        this.identityProviderService = identityProviderService;
        this.formRevisionService = formRevisionService;
        this.formVersionWithDetailsRepository = formVersionWithDetailsRepository;
        this.formVersionRepository = formVersionRepository;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Identity Providers",
            description = "Retrieves a paginated list of identity providers based on the provided filters."
    )
    public Page<IdentityProviderListDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid IdentityProviderFilter filter
    ) throws ResponseException {
        return identityProviderService
                .list(pageable, filter)
                .map(IdentityProviderListDTO::from);
    }

    @PostMapping("prepare/")
    @Operation(
            summary = "Prepare Identity Provider",
            description = "Prepares an identity provider by validating the provided endpoint and retrieving necessary metadata."
    )
    public IdentityProviderDetailsDTO prepare(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody IdentityProviderPrepareDTO requestDTO
    ) throws ResponseException {
        userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var preparedEntity = identityProviderService
                .prepare(requestDTO.endpoint());

        return IdentityProviderDetailsDTO
                .from(preparedEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Identity Provider",
            description = "Creates a new identity provider with the provided configuration. " +
                          "Only system administrators are allowed to perform this action."
    )
    public IdentityProviderDetailsDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody IdentityProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var created = identityProviderService
                .create(requestDTO.toEntity());

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Create, this.getClass().getSimpleName(), IdentityProviderEntity.class, "legacy", "legacy", Map.of(
                                "key", created.getKey(),
                                "name", created.getName()
                        )));

        return IdentityProviderDetailsDTO
                .from(created);
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Identity Provider",
            description = "Retrieves the details of a specific identity provider by its unique key."
    )
    public IdentityProviderDetailsDTO retrieve(
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        return identityProviderService
                .retrieve(key)
                .map(IdentityProviderDetailsDTO::from)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Identity Provider",
            description = "Updates the configuration of an existing identity provider. " +
                          "If the provider is disabled, it will be unlinked from all forms that use it. " +
                          "Only system administrators are allowed to perform this action."
    )
    public IdentityProviderDetailsDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid IdentityProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var formFilter = VFormVersionWithDetailsFilter
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
            var linkedFormFilter = VFormVersionWithDetailsFilter
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
                formVersionRepository.save(form.toFormVersionEntity());

                formRevisionService
                        .create(user, form, formClone);
            }
        }

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), IdentityProviderEntity.class, "legacy", "legacy", Map.of(
                "key", updatedEntity.getKey(),
                "name", updatedEntity.getName()
        )));

        return IdentityProviderDetailsDTO
                .from(updatedEntity);
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Identity Provider",
            description = "Deletes an identity provider if it is disabled and not linked to any published forms. " +
                          "Only system administrators are allowed to perform this action."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var entity = identityProviderService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        if (entity.getIsEnabled()) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s kann nicht gelöscht werden, da er aktiviert ist.",
                    key
            );
        }

        var formFilter = new VFormVersionWithDetailsFilter()
                .setIdentityProviderKey(key)
                .setStatus(FormStatus.Published);

        if (formVersionWithDetailsRepository.exists(formFilter.build())) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s kann nicht gelöscht werden, da veröffentlichte Formulare existieren, die diesen Anbieter verwenden.",
                    key
            );
        }

        var linkedFormFilter = new VFormVersionWithDetailsFilter()
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
            formVersionRepository.save(form.toFormVersionEntity());

            formRevisionService
                    .create(user, form, formClone);
        }


        var deletedEntity = identityProviderService
                .delete(key);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Delete, this.getClass().getSimpleName(), IdentityProviderEntity.class, "legacy", "legacy", Map.of(
                "key", deletedEntity.getKey(),
                "name", deletedEntity.getName()
        )));
    }
}

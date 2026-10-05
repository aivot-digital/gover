package de.aivot.prosuna.backend.customLink.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.prosuna.backend.customLink.dtos.CustomLinkOrderRequestDTO;
import de.aivot.prosuna.backend.customLink.dtos.CustomLinkRequestDTO;
import de.aivot.prosuna.backend.customLink.dtos.CustomLinkResponseDTO;
import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.filters.CustomLinkFilter;
import de.aivot.prosuna.backend.customLink.services.CustomLinkService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-links/")
@Tag(name = "Custom Links", description = "Configurable links displayed in supported application contexts.")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class CustomLinkController {
    private final ScopedAuditService auditService;
    private final CustomLinkService service;
    private final UserService userService;
    private final PermissionService permissionService;

    public CustomLinkController(AuditService auditService,
                                CustomLinkService service,
                                UserService userService,
                                PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(CustomLinkController.class, "Custom-Links");
        this.service = service;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    public Page<CustomLinkResponseDTO> list(@Nullable @AuthenticationPrincipal Jwt jwt,
                                            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
                                            @Nonnull @ParameterObject @Valid CustomLinkFilter filter) throws ResponseException {
        permissionService.requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);
        return service.list(pageable, filter).map(CustomLinkResponseDTO::fromEntity);
    }

    @GetMapping("available/")
    public Page<CustomLinkResponseDTO> listAvailable(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                     @Nonnull @ParameterObject @PageableDefault Pageable pageable,
                                                     @Nonnull @ParameterObject @Valid CustomLinkFilter filter) throws ResponseException {
        userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        filter.setEnabled(true);
        return service.list(pageable, filter).map(CustomLinkResponseDTO::fromEntity);
    }

    @PostMapping("")
    public CustomLinkResponseDTO create(@Nullable @AuthenticationPrincipal Jwt jwt,
                                        @Nonnull @Valid @RequestBody CustomLinkRequestDTO request) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        permissionService.requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_CREATE);
        var link = service.create(request.toEntity());
        auditService.create().withUser(user).withAuditAction(AuditAction.Create, CustomLink.class, link.getId(), "id", Map.of(
                "label", link.getLabel(), "url", link.getUrl()
        )).withMessage("Der Custom-Link %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(link.getLabel()), StringUtils.quote(user.getFullName())).log();
        return CustomLinkResponseDTO.fromEntity(link);
    }

    @GetMapping("{id}/")
    public CustomLinkResponseDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                          @Nonnull @PathVariable Integer id) throws ResponseException {
        permissionService.requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);
        return service.retrieve(id).map(CustomLinkResponseDTO::fromEntity).orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public CustomLinkResponseDTO update(@Nullable @AuthenticationPrincipal Jwt jwt,
                                        @Nonnull @PathVariable Integer id,
                                        @Nonnull @Valid @RequestBody CustomLinkRequestDTO request) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        permissionService.requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);
        var link = service.update(id, request.toEntity());
        auditService.create().withUser(user).withAuditAction(AuditAction.Update, CustomLink.class, link.getId(), "id", Map.of(
                "label", link.getLabel(), "url", link.getUrl()
        )).withMessage("Der Custom-Link %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(link.getLabel()), StringUtils.quote(user.getFullName())).log();
        return CustomLinkResponseDTO.fromEntity(link);
    }

    @PutMapping("order/")
    public List<CustomLinkResponseDTO> reorder(@Nullable @AuthenticationPrincipal Jwt jwt,
                                               @Nonnull @Valid @RequestBody CustomLinkOrderRequestDTO request) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        permissionService.requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);
        var links = service.reorder(request.type(), request.ids());
        auditService
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Update,
                        CustomLink.class,
                        request.type().name(),
                        "type"
                )
                .withMessage("Die Reihenfolge der Custom-Links vom Typ %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(request.type().name()),
                        StringUtils.quote(user.getFullName())).log();
        return links.stream().map(CustomLinkResponseDTO::fromEntity).toList();
    }

    @DeleteMapping("{id}/")
    public void delete(@Nullable @AuthenticationPrincipal Jwt jwt,
                       @Nonnull @PathVariable Integer id) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        permissionService.requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_DELETE);
        var link = service.delete(id);
        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, CustomLink.class, link.getId(), "id", Map.of(
                "label", link.getLabel(), "url", link.getUrl()
        )).withMessage("Der Custom-Link %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(link.getLabel()), StringUtils.quote(user.getFullName())).log();
    }
}

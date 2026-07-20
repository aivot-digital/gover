package de.aivot.gover.backend.preset.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.preset.dtos.PresetCreateRequestDTO;
import de.aivot.gover.backend.preset.entities.PresetEntity;
import de.aivot.gover.backend.preset.filters.PresetFilter;
import de.aivot.gover.backend.preset.permissions.PresetPermissionProvider;
import de.aivot.gover.backend.preset.repositories.PresetRepository;
import de.aivot.gover.backend.preset.repositories.PresetVersionRepository;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/presets/")
@Tag(
        name = "Presets",
        description = "Presets are prebuilt elements which can be used in element builders."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PresetController {
    private final PresetRepository presetRepository;
    private final ScopedAuditService auditService;
    private final PresetVersionRepository presetVersionRepository;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public PresetController(PresetRepository presetRepository,
                            AuditService auditService,
                            PresetVersionRepository presetVersionRepository,
                            UserService userService,
                            PermissionService permissionService) {
        this.presetRepository = presetRepository;
        this.auditService = auditService.createScopedAuditService(PresetController.class, "Vorlagen");
        this.presetVersionRepository = presetVersionRepository;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Presets",
            description = "Retrieve a paginated list of presets with optional filtering. " +
                    "This requires the permission „" + PresetPermissionProvider.PRESET_READ + "“."
    )
    public Page<PresetEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid PresetFilter filter
    ) throws ResponseException {
        permissionService
                .hasSystemPermission(jwt, PresetPermissionProvider.PRESET_READ);

        return presetRepository
                .findAll(filter.build(), pageable);
    }


    @PostMapping("")
    @Operation(
            summary = "Create Preset",
            description = "Create a new preset. " +
                    "This requires the permission „" + PresetPermissionProvider.PRESET_CREATE + "“."
    )
    public PresetEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody PresetCreateRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermission(user.getId(), PresetPermissionProvider.PRESET_CREATE);

        var newEntity = requestDTO
                .toEntity();

        var savedEntity = presetRepository
                .save(newEntity);

        var newVersion = requestDTO
                .toVersionEntity(savedEntity);

        var savedVersion = presetVersionRepository
                .save(newVersion);

        auditService.create().withUser(user).withAuditAction(AuditAction.Create, PresetEntity.class, savedEntity.getKey(), "key", Map.of(
                                "key", savedEntity.getKey(),
                                "title", savedEntity.getTitle(),
                                "version", savedVersion.getVersion()
                        )).withMessage(
                "Die Vorlage %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s erstellt (Version %s).",
                StringUtils.quote(savedEntity.getTitle()),
                StringUtils.quote(String.valueOf(savedEntity.getKey())),
                StringUtils.quote(user.getFullName()),
                StringUtils.quote(String.valueOf(savedVersion.getVersion()))
        ).log();

        return presetRepository
                .findById(savedEntity.getKey())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Preset",
            description = "Retrieve a specific preset by its unique key. " +
                    "This requires the permission „" + PresetPermissionProvider.PRESET_READ + "“."
    )
    public PresetEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        permissionService
                .hasSystemPermission(jwt, PresetPermissionProvider.PRESET_READ);

        return presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Update a preset.
     *
     * @param jwt                 The JWT of the user.
     * @param key                 The key of the preset to update.
     * @param updatedPresetEntity The updated preset.
     * @return The updated preset.
     */
    @PutMapping("{key}/")
    @Operation(
            summary = "Update Preset",
            description = "Update an existing preset. " +
                    "This requires the permission „" + PresetPermissionProvider.PRESET_UPDATE + "“."
    )
    public PresetEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @Valid @RequestBody PresetEntity updatedPresetEntity
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermission(user.getId(), PresetPermissionProvider.PRESET_UPDATE);

        var preset = presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        preset.setTitle(updatedPresetEntity.getTitle());
        preset.setUpdated(Instant.now());

        var savePreset = presetRepository.save(preset);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("key", savePreset.getKey());
        auditData.put("title", savePreset.getTitle());

        auditService.create().withUser(user).withAuditAction(AuditAction.Update, PresetEntity.class, savePreset.getKey(), "key", auditData).withMessage(
                "Die Vorlage %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(savePreset.getTitle()),
                StringUtils.quote(String.valueOf(savePreset.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();

        return savePreset;
    }

    /**
     * Delete a preset.
     *
     * @param jwt The JWT of the user.
     * @param key The key of the preset to delete.
     */
    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Preset",
            description = "Delete an existing preset. " +
                    "This requires the permission „" + PresetPermissionProvider.PRESET_DELETE + "“."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermission(user.getId(), PresetPermissionProvider.PRESET_DELETE);

        var preset = presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        presetRepository.delete(preset);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, PresetEntity.class, preset.getKey(), "key", Map.of(
                        "key", preset.getKey(),
                        "title", preset.getTitle()
                )).withMessage(
                "Die Vorlage %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(preset.getTitle()),
                StringUtils.quote(String.valueOf(preset.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}

package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.dtos.PresetCreateRequestDTO;
import de.aivot.GoverBackend.preset.entities.PresetEntity;
import de.aivot.GoverBackend.preset.filters.PresetFilter;
import de.aivot.GoverBackend.preset.repositories.PresetRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionRepository;
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

import java.time.LocalDateTime;
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

    @Autowired
    public PresetController(PresetRepository presetRepository,
                            AuditService auditService,
                            PresetVersionRepository presetVersionRepository, UserService userService) {
        this.presetRepository = presetRepository;
        this.auditService = auditService.createScopedAuditService(PresetController.class);
        this.presetVersionRepository = presetVersionRepository;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Presets",
            description = "Retrieve a paginated list of presets with optional filtering."
    )
    public Page<PresetEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid PresetFilter filter
    ) throws ResponseException {
        return presetRepository
                .findAll(filter.build(), pageable);
    }


    @PostMapping("")
    @Operation(
            summary = "Create Preset",
            description = "Create a new preset."
    )
    public PresetEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody PresetCreateRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var newEntity = requestDTO
                .toEntity();

        var savedEntity = presetRepository
                .save(newEntity);

        var newVersion = requestDTO
                .toVersionEntity(savedEntity);

        var savedVersion = presetVersionRepository
                .save(newVersion);

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        PresetEntity.class,
                        Map.of(
                                "key", savedEntity.getKey(),
                                "title", savedEntity.getTitle(),
                                "version", savedVersion.getVersion()
                        )
                );

        return presetRepository
                .findById(savedEntity.getKey())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Preset",
            description = "Retrieve a specific preset by its unique key."
    )
    public PresetEntity retrieve(
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
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
            description = "Update an existing preset."
    )
    public PresetEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @Valid @RequestBody PresetEntity updatedPresetEntity
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        preset.setTitle(updatedPresetEntity.getTitle());
        preset.setUpdated(LocalDateTime.now());

        var savePreset = presetRepository.save(preset);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("key", savePreset.getKey());
        auditData.put("title", savePreset.getTitle());

        auditService.logAction(
                user,
                AuditAction.Update,
                PresetEntity.class,
                auditData
        );

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
            description = "Delete an existing preset."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        presetRepository.delete(preset);

        auditService.logAction(
                user,
                AuditAction.Delete,
                PresetEntity.class,
                Map.of(
                        "key", preset.getKey(),
                        "title", preset.getTitle()
                )
        );
    }
}

package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.entities.PresetEntity;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntity;
import de.aivot.GoverBackend.preset.filters.PresetFilter;
import de.aivot.GoverBackend.preset.repositories.PresetRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionRepository;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/presets/")
public class PresetController {
    private final PresetRepository presetRepository;
    private final ScopedAuditService auditService;
    private final PresetVersionRepository presetVersionRepository;

    @Autowired
    public PresetController(
            PresetRepository presetRepository,
            AuditService auditService,
            PresetVersionRepository presetVersionRepository) {
        this.presetRepository = presetRepository;
        this.auditService = auditService.createScopedAuditService(PresetController.class);
        this.presetVersionRepository = presetVersionRepository;
    }

    /**
     * List all presets.
     *
     * @param jwt      The JWT of the user.
     * @param pageable The pagination information.
     * @param filter   The filter to apply.
     * @return The page of presets.
     */
    @GetMapping("")
    public Page<PresetEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid PresetFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return presetRepository
                .findAll(filter.build(), pageable);
    }

    /**
     * Create a new preset.
     *
     * @param newPresetEntity The preset to create.
     * @return The created preset.
     */
    @PostMapping("")
    public PresetEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody PresetEntity newPresetEntity
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        newPresetEntity.setKey(UUID.randomUUID());
        newPresetEntity.setPublishedVersion(null);
        newPresetEntity.setDraftedVersion(null);
        newPresetEntity.setCreated(LocalDateTime.now());
        newPresetEntity.setUpdated(LocalDateTime.now());

        var entity = presetRepository
                .save(newPresetEntity);

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        PresetEntity.class,
                        Map.of(
                                "key", entity.getKey(),
                                "title", entity.getTitle()
                        )
                );

        var initialPresetVersion = new PresetVersionEntity(
                entity.getKey(),
                1,
                new GroupLayout(),
                FormStatus.Drafted,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );
        presetVersionRepository.save(initialPresetVersion);

        return presetRepository
                .findById(entity.getKey())
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Retrieve a preset.
     *
     * @param jwt The JWT of the user.
     * @param key The key of the preset to retrieve.
     * @return The preset.
     */
    @GetMapping("{key}/")
    public PresetEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

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
    public PresetEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @Valid @RequestBody PresetEntity updatedPresetEntity
    ) throws ResponseException {
        var user = UserService
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
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = UserService
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

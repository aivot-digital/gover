package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.entities.Preset;
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
    private final PresetVersionRepository versionRepository;
    private final ScopedAuditService auditService;

    @Autowired
    public PresetController(
            PresetRepository presetRepository,
            PresetVersionRepository versionRepository,
            AuditService auditService
    ) {
        this.presetRepository = presetRepository;
        this.versionRepository = versionRepository;
        this.auditService = auditService.createScopedAuditService(PresetController.class);
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
    public Page<Preset> list(
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
     * @param newPreset The preset to create.
     * @return The created preset.
     */
    @PostMapping("")
    public Preset create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody Preset newPreset
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var key = UUID
                .randomUUID()
                .toString();

        var spec = PresetFilter
                .create()
                .setExactTitle(newPreset.getTitle())
                .build();

        if (presetRepository.exists(spec)) {
            throw ResponseException.conflict("Es existiert bereits eine Vorlage mit diesem Titel.");
        }

        newPreset.setKey(key);
        newPreset.setStoreId(null);
        newPreset.setCurrentVersion(null);
        newPreset.setCurrentStoreVersion(null);
        newPreset.setCurrentPublishedVersion(null);
        newPreset.setCreated(LocalDateTime.now());
        newPreset.setUpdated(LocalDateTime.now());

        var entity = presetRepository
                .save(newPreset);

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        Preset.class,
                        Map.of(
                                "key", entity.getKey(),
                                "title", entity.getTitle()
                        )
                );

        return entity;
    }

    /**
     * Retrieve a preset.
     *
     * @param jwt The JWT of the user.
     * @param key The key of the preset to retrieve.
     * @return The preset.
     */
    @GetMapping("{key}/")
    public Preset retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
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
     * @param jwt           The JWT of the user.
     * @param key           The key of the preset to update.
     * @param updatedPreset The updated preset.
     * @return The updated preset.
     */
    @PutMapping("{key}/")
    public Preset update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @Valid @RequestBody Preset updatedPreset
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        preset.setStoreId(updatedPreset.getStoreId());
        preset.setUpdated(LocalDateTime.now());

        var savePreset = presetRepository.save(preset);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("key", savePreset.getKey());
        auditData.put("title", savePreset.getTitle());
        if (savePreset.getStoreId() != null) {
            auditData.put("storeId", savePreset.getStoreId());
        }

        auditService.logAction(
                user,
                AuditAction.Update,
                Preset.class,
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
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = presetRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        var versions = versionRepository.findAllByPreset(key, pageable);
        versionRepository.deleteAll(versions);

        presetRepository.delete(preset);

        auditService.logAction(
                user,
                AuditAction.Delete,
                Preset.class,
                Map.of(
                        "key", preset.getKey(),
                        "title", preset.getTitle()
                )
        );
    }
}

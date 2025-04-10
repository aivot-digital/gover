package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.entities.Preset;
import de.aivot.GoverBackend.preset.entities.PresetVersion;
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
import java.util.Map;

@RestController
@RequestMapping("/api/presets/{presetKey}/versions/")
public class PresetVersionController {
    private final PresetRepository repository;
    private final PresetVersionRepository versionRepository;
    private final ScopedAuditService auditService;

    @Autowired
    public PresetVersionController(
            PresetRepository repository,
            PresetVersionRepository versionRepository,
            AuditService auditService
    ) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.auditService = auditService.createScopedAuditService(PresetController.class);

    }

    /**
     * List all versions of a preset.
     *
     * @param jwt      The JWT of the user.
     * @param pageable The pagination information.
     * @param presetKey The key of the preset.
     * @return The page of versions.
     */
    @GetMapping("")
    public Page<PresetVersion> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @PathVariable String presetKey
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::badRequest);

        return versionRepository
                .findAllByPreset(preset.getKey(), pageable);
    }

    /**
     * Create a new version of a preset.
     *
     * @param jwt       The JWT of the user.
     * @param presetKey The key of the preset.
     * @param newVersion The new version.
     * @return The new version.
     */
    @PostMapping("")
    public PresetVersion createVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String presetKey,
            @Valid @RequestBody PresetVersion newVersion
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::badRequest);

        boolean exists = versionRepository
                .existsByPresetAndVersion(
                        preset.getKey(),
                        newVersion.getVersion()
                );
        if (exists) {
            throw ResponseException.conflict("Diese Vorlagen-Version existiert bereits.");
        }

        var savedVersion = versionRepository.save(newVersion);

        preset.setCurrentVersion(savedVersion.getVersion());
        repository.save(preset);

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        PresetVersion.class,
                        Map.of(
                                "id", preset.getKey(),
                                "title", preset.getTitle(),
                                "version", savedVersion.getVersion()
                        )
                );

        return savedVersion;
    }

    /**
     * Retrieve a version of a preset.
     *
     * @param presetKey     The key of the preset.
     * @param version The version.
     * @return The version.
     */
    @GetMapping("{version}/")
    public PresetVersion retrieveVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String presetKey,
            @Nonnull @PathVariable String version
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return versionRepository
                .getByPresetAndVersion(presetKey, version)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Update a version of a preset.
     *
     * @param jwt       The JWT of the user.
     * @param presetKey The key of the preset.
     * @param version   The version.
     * @param updatedPreset The updated version.
     * @return The updated version.
     */
    @PutMapping("{version}/")
    public PresetVersion updateVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String presetKey,
            @Nonnull @PathVariable String version,
            @Valid @RequestBody PresetVersion updatedPreset
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::badRequest);

        var presetVersion = versionRepository
                .getByPresetAndVersion(presetKey, version)
                .orElseThrow(ResponseException::notFound);

        if (presetVersion.getPublishedAt() == null && presetVersion.getPublishedStoreAt() == null) {
            presetVersion.setRoot(updatedPreset.getRoot());
        }

        if (presetVersion.getPublishedAt() == null && updatedPreset.getPublishedAt() != null) {
            presetVersion.setPublishedAt(updatedPreset.getPublishedAt());

            preset.setCurrentPublishedVersion(presetVersion.getVersion());
            repository.save(preset);
        }

        if (presetVersion.getPublishedStoreAt() == null && updatedPreset.getPublishedStoreAt() != null) {
            presetVersion.setPublishedStoreAt(updatedPreset.getPublishedStoreAt());

            preset.setCurrentStoreVersion(presetVersion.getVersion());
            repository.save(preset);
        }

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        PresetVersion.class,
                        Map.of(
                                "id", preset.getKey(),
                                "title", preset.getTitle(),
                                "version", presetVersion.getVersion()
                        )
                );

        return versionRepository.save(presetVersion);
    }

    /**
     * Destroy a version of a preset. Destroy preset if no versions left.
     *
     * @param jwt       The JWT of the user.
     * @param presetKey The key of the preset.
     * @param version   The version.
     */
    @DeleteMapping("{version}/")
    public void destroyVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String presetKey,
            @Nonnull @PathVariable String version,
            Pageable pageable) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::notFound);

        var presetVersion = versionRepository
                .getByPresetAndVersion(presetKey, version)
                .orElseThrow(ResponseException::notFound);

        if (presetVersion.getPublishedAt() != null || presetVersion.getPublishedStoreAt() != null) {
            throw ResponseException.conflict("Veröffentlichte Versionen können nicht gelöscht werden.");
        }

        versionRepository.delete(presetVersion);

        var otherVersions = versionRepository.findAllByPreset(presetKey, pageable);

        if (otherVersions.isEmpty()) {
            repository.delete(preset);
        } else {
            var otherVersion = otherVersions.iterator().next();
            preset.setCurrentVersion(otherVersion.getVersion());
            repository.save(preset);
        }

        auditService.logAction(
                user,
                AuditAction.Delete,
                Preset.class,
                Map.of(
                        "key", preset.getKey(),
                        "title", preset.getTitle(),
                        "version", presetVersion.getVersion()
                )
        );
    }
}

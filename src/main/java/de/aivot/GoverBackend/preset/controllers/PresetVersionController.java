package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.entities.PresetEntity;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntity;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntityId;
import de.aivot.GoverBackend.preset.filters.PresetVersionFilter;
import de.aivot.GoverBackend.preset.repositories.PresetRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionWithDetailsRepository;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/presets/{presetKey}/versions/")
@Tag(
        name = "Preset Versions",
        description = "Endpoints for managing preset versions"
)
public class PresetVersionController {
    private final PresetRepository repository;
    private final PresetVersionRepository versionRepository;
    private final ScopedAuditService auditService;
    private final PresetVersionWithDetailsRepository presetVersionWithDetailsRepository;
    private final UserService userService;

    @Autowired
    public PresetVersionController(PresetRepository repository,
                                   PresetVersionRepository versionRepository,
                                   AuditService auditService,
                                   PresetVersionWithDetailsRepository presetVersionWithDetailsRepository,
                                   UserService userService) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.auditService = auditService.createScopedAuditService(PresetController.class);
        this.presetVersionWithDetailsRepository = presetVersionWithDetailsRepository;
        this.userService = userService;
    }

    /**
     * List all versions of a preset.
     *
     * @param pageable  The pagination information.
     * @param presetKey The key of the preset.
     * @return The page of versions.
     */
    @GetMapping("")
    @Operation(
            summary = "List Preset Versions",
            description = "Retrieve a paginated list of versions for a specific preset with optional filtering."
    )
    public Page<PresetVersionEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid PresetVersionFilter filter,
            @Nonnull @PathVariable UUID presetKey
    ) throws ResponseException {
        filter.setPresetKey(presetKey);

        return presetVersionWithDetailsRepository
                .findAll(filter.build(), pageable);
    }

    /**
     * Create a new version of a preset.
     *
     * @param jwt        The JWT of the user.
     * @param presetKey  The key of the preset.
     * @param newVersion The new version.
     * @return The new version.
     */
    @PostMapping("")
    @Operation(
            summary = "Create Preset Version",
            description = "Create a new version for a specific preset."
    )
    public PresetVersionEntity createVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID presetKey,
            @Valid @RequestBody PresetVersionEntity newVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::badRequest);

        var newVersionNumber = versionRepository
                                       .maxVersionForPresetKey(preset.getKey())
                                       .orElse(0) + 1;

        newVersion.setPresetKey(presetKey);
        newVersion.setVersion(newVersionNumber);
        newVersion.setStatus(FormStatus.Drafted);
        newVersion.setCreated(null);
        newVersion.setUpdated(null);
        newVersion.setPublished(null);
        newVersion.setRevoked(null);

        var savedVersion = versionRepository
                .save(newVersion);

        auditService
                .logAction(
                        user,
                        AuditAction.Create,
                        PresetVersionEntity.class,
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
     * @param presetKey The key of the preset.
     * @param version   The version.
     * @return The version.
     */
    @GetMapping("{version}/")
    @Operation(
            summary = "Retrieve Preset Version",
            description = "Retrieve a specific version of a preset by its version number."
    )
    public PresetVersionEntity retrieveVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID presetKey,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var id = new PresetVersionEntityId(presetKey, version);

        return versionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Update a version of a preset.
     *
     * @param jwt           The JWT of the user.
     * @param presetKey     The key of the preset.
     * @param version       The version.
     * @param updatedPreset The updated version.
     * @return The updated version.
     */
    @PutMapping("{version}/")
    @Operation(
            summary = "Update Preset Version",
            description = "Update an existing version of a preset."
    )
    public PresetVersionEntity updateVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID presetKey,
            @Nonnull @PathVariable Integer version,
            @Valid @RequestBody PresetVersionEntity updatedPreset
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::badRequest);

        var id = new PresetVersionEntityId(presetKey, version);

        var presetVersion = versionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        if (presetVersion.getStatus() != FormStatus.Drafted) {
            throw ResponseException.conflict("Veröffentlichte Versionen können nicht bearbeitet werden.");
        }

        presetVersion.setRootElement(updatedPreset.getRootElement());

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        PresetVersionEntity.class,
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
    @Operation(
            summary = "Delete Preset Version",
            description = "Delete a specific version of a preset. Published versions cannot be deleted."
    )
    public void destroyVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID presetKey,
            @Nonnull @PathVariable Integer version,
            Pageable pageable) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var preset = repository
                .findById(presetKey)
                .orElseThrow(ResponseException::notFound);

        var id = new PresetVersionEntityId(presetKey, version);

        var presetVersion = versionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        if (presetVersion.getStatus() == FormStatus.Published) {
            throw ResponseException.conflict("Veröffentlichte Versionen können nicht gelöscht werden.");
        }

        versionRepository.delete(presetVersion);

        auditService.logAction(
                user,
                AuditAction.Delete,
                PresetEntity.class,
                Map.of(
                        "key", preset.getKey(),
                        "title", preset.getTitle(),
                        "version", presetVersion.getVersion()
                )
        );
    }

    @PutMapping("{version}/publish/")
    @Operation(
            summary = "Publish Preset Version",
            description = "Publish a specific version of a preset."
    )
    public PresetVersionEntity publishVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID presetKey,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var id = new PresetVersionEntityId(presetKey, version);

        var presetVersion = versionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        if (presetVersion.getStatus() == FormStatus.Published) {
            throw ResponseException.conflict("Die Version ist bereits veröffentlicht.");
        }

        presetVersion.setStatus(FormStatus.Published);

        auditService.logAction(
                user,
                AuditAction.Update,
                PresetVersionEntity.class,
                Map.of(
                        "key", presetVersion.getPresetKey(),
                        "version", presetVersion.getVersion(),
                        "status", presetVersion.getStatus().toString()
                )
        );

        return versionRepository.save(presetVersion);
    }

    @PutMapping("{version}/revoke/")
    @Operation(
            summary = "Revoke Preset Version",
            description = "Revoke a specific version of a preset."
    )
    public PresetVersionEntity revokeVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID presetKey,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var id = new PresetVersionEntityId(presetKey, version);

        var presetVersion = versionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        if (presetVersion.getStatus() == FormStatus.Revoked) {
            throw ResponseException.conflict("Die Version ist bereits widerrufen.");
        }

        presetVersion.setStatus(FormStatus.Revoked);

        auditService.logAction(
                user,
                AuditAction.Update,
                PresetVersionEntity.class,
                Map.of(
                        "key", presetVersion.getPresetKey(),
                        "version", presetVersion.getVersion(),
                        "status", presetVersion.getStatus().toString()
                )
        );

        return versionRepository.save(presetVersion);
    }
}

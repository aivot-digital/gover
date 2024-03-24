package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.BadRequestException;
import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.entities.PresetVersion;
import de.aivot.GoverBackend.repositories.PresetRepository;
import de.aivot.GoverBackend.repositories.PresetVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;

@RestController
public class PresetVersionController {
    private final PresetRepository repository;
    private final PresetVersionRepository versionRepository;

    @Autowired
    public PresetVersionController(
            PresetRepository repository,
            PresetVersionRepository versionRepository
    ) {
        this.repository = repository;
        this.versionRepository = versionRepository;
    }

    /**
     * List all versions of a preset.
     *
     * @param presetKey The key of the preset.
     * @return A collection of versions.
     */
    @GetMapping("/api/preset-versions")
    public Collection<PresetVersion> listVersions(
            @RequestParam(name = "preset", required = false) String presetKey
    ) {
        return versionRepository.findAllByPreset(presetKey);
    }

    /**
     * Create a new version of a preset and set the reference in the preset to the new version.
     *
     * @param newVersion The new version.
     * @return The created version.
     */
    @PostMapping("/api/preset-versions")
    public PresetVersion createVersion(
            @Valid @RequestBody PresetVersion newVersion
    ) {
        var preset = repository
                .findById(newVersion.getPreset())
                .orElseThrow(BadRequestException::new);

        boolean exists = versionRepository
                .existsByPresetAndVersion(
                        preset.getKey(),
                        newVersion.getVersion()
                );
        if (exists) {
            throw new ConflictException();
        }

        var savedVersion = versionRepository.save(newVersion);

        preset.setCurrentVersion(savedVersion.getVersion());
        repository.save(preset);

        return savedVersion;
    }

    /**
     * Retrieve a version of a preset.
     *
     * @param presetKey     The key of the preset.
     * @param versionString The version.
     * @return The version.
     */
    @GetMapping("/api/preset-versions/{preset}/{version}")
    public PresetVersion retrieveVersion(
            @PathVariable(name = "preset") String presetKey,
            @PathVariable(name = "version") String versionString
    ) {
        return versionRepository
                .getByPresetAndVersion(presetKey, versionString)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Update a version of a preset. Update the preset reference if the version is published.
     *
     * @param presetKey     The key of the preset.
     * @param versionString The version.
     * @param updatedPreset The updated version.
     * @return The updated version.
     */
    @PutMapping("/api/preset-versions/{preset}/{version}")
    public PresetVersion updateVersion(
            @PathVariable(name = "preset") String presetKey,
            @PathVariable(name = "version") String versionString,
            @Valid @RequestBody PresetVersion updatedPreset
    ) {
        var preset = repository
                .findById(presetKey)
                .orElseThrow(BadRequestException::new);

        var presetVersion = versionRepository
                .getByPresetAndVersion(presetKey, versionString)
                .orElseThrow(NotFoundException::new);

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

        return versionRepository.save(presetVersion);
    }

    /**
     * Delete a version of a preset. Delete the preset if it has no versions left.
     *
     * @param presetKey     The key of the preset.
     * @param versionString The version.
     */
    @DeleteMapping("/api/preset-versions/{preset}/{version}")
    public void destroyVersion(
            @PathVariable(name = "preset") String presetKey,
            @PathVariable(name = "version") String versionString
    ) {
        var preset = repository
                .findById(presetKey)
                .orElseThrow(NotFoundException::new);

        var presetVersion = versionRepository
                .getByPresetAndVersion(presetKey, versionString)
                .orElseThrow(NotFoundException::new);

        if (presetVersion.getPublishedAt() != null || presetVersion.getPublishedStoreAt() != null) {
            throw new ConflictException();
        }

        versionRepository.delete(presetVersion);

        var otherVersions = versionRepository.findAllByPreset(presetKey);

        if (otherVersions.isEmpty()) {
            repository.delete(preset);
        } else {
            var otherVersion = otherVersions.iterator().next();
            preset.setCurrentVersion(otherVersion.getVersion());
            repository.save(preset);
        }
    }
}

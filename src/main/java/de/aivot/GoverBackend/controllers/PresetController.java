package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.entities.Preset;
import de.aivot.GoverBackend.repositories.PresetRepository;
import de.aivot.GoverBackend.repositories.PresetVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@RestController
public class PresetController {
    private final PresetRepository presetRepository;
    private final PresetVersionRepository versionRepository;

    @Autowired
    public PresetController(
            PresetRepository presetRepository,
            PresetVersionRepository versionRepository
    ) {
        this.presetRepository = presetRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * List all presets.
     *
     * @param onlyPublished Limit listed presets to those that have a published version.
     * @return A collection of presets.
     */
    @GetMapping("/api/presets")
    public Collection<Preset> list(
            @RequestParam(name = "published", required = false) Boolean onlyPublished
    ) {
        if (Boolean.TRUE.equals(onlyPublished)) {
            return presetRepository.findAllByCurrentPublishedVersionIsNotNull();
        }
        return presetRepository.findAll();
    }

    /**
     * Create a new preset.
     *
     * @param newPreset The preset to create.
     * @return The created preset.
     */
    @PostMapping("/api/presets")
    public Preset create(
            @Valid @RequestBody Preset newPreset
    ) {
        var key = UUID.randomUUID().toString();

        if (presetRepository.existsById(key)) {
            throw new ConflictException("Duplicate key");
        }

        if (presetRepository.existsByTitle(newPreset.getTitle())) {
            throw new ConflictException("Preset with title already exists");
        }

        newPreset.setKey(key);
        newPreset.setStoreId(null);
        newPreset.setCurrentVersion(null);
        newPreset.setCurrentStoreVersion(null);
        newPreset.setCurrentPublishedVersion(null);
        newPreset.setCreated(LocalDateTime.now());
        newPreset.setUpdated(LocalDateTime.now());

        return presetRepository.save(newPreset);
    }

    /**
     * Retrieve a preset.
     *
     * @param key The key of the preset to retrieve.
     * @return The preset.
     */
    @GetMapping("/api/presets/{key}")
    public Preset retrieve(
            @PathVariable String key
    ) {
        return presetRepository
                .findById(key)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Update a preset.
     *
     * @param key           The key of the preset to update.
     * @param updatedPreset The updated preset.
     * @return The updated preset.
     */
    @PutMapping("/api/presets/{key}")
    public Preset update(
            @PathVariable String key,
            @Valid @RequestBody Preset updatedPreset
    ) {
        var preset = presetRepository
                .findById(key)
                .orElseThrow(NotFoundException::new);

        preset.setStoreId(updatedPreset.getStoreId());
        preset.setUpdated(LocalDateTime.now());

        return presetRepository.save(preset);
    }

    /**
     * Delete a preset and all its versions.
     *
     * @param key The key of the preset to delete.
     */
    @DeleteMapping("/api/presets/{key}")
    public void destroy(
            @PathVariable String key
    ) {
        var preset = presetRepository
                .findById(key)
                .orElseThrow(NotFoundException::new);

        var versions = versionRepository.findAllByPreset(key);
        versionRepository.deleteAll(versions);

        presetRepository.delete(preset);
    }
}

package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.entities.Preset;
import de.aivot.GoverBackend.repositories.PresetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class PresetController {
    private final PresetRepository repository;

    @Autowired
    public PresetController(PresetRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/presets")
    public Collection<Preset> list() {
        return repository.findAll();
    }

    @PostMapping("/api/presets")
    public Preset create(
            @RequestBody Preset newLink
    ) {
        return repository.save(newLink);
    }

    @GetMapping("/api/presets/{id}")
    public Preset retrieve(@PathVariable Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/api/presets/{id}")
    public Preset update(
            @PathVariable Integer id,
            @RequestBody Preset updatedLink
    ) {
        Optional<Preset> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Preset preset = optLink.get();
        preset.setRoot(updatedLink.getRoot());

        return repository.save(preset);
    }

    @DeleteMapping("/api/presets/{id}")
    public void destroy(
            @PathVariable Integer id
    ) {
        Optional<Preset> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        repository.delete(optLink.get());
    }
}

package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
public class SystemConfigController {
    private final SystemConfigRepository repository;

    @Autowired
    public SystemConfigController(SystemConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/system-configs")
    public Collection<SystemConfig> list() {
        return repository.findAll();
    }

    @GetMapping("/api/public/system-configs")
    public Collection<SystemConfig> listPublic() {
        return repository.findSystemConfigsByPublicConfigIsTrue();
    }

    @IsAdmin
    @PutMapping("/api/system-configs/{id}")
    public SystemConfig update(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody SystemConfig config
    ) {
        return repository
                .findById(id)
                .map(existingConfig -> {
                    existingConfig.setValue(config.getValue());
                    existingConfig.setPublicConfig(config.isPublicConfig());
                    return repository.save(existingConfig);
                })
                .orElseGet(() -> repository.save(config));
    }
}

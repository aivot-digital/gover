package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
public class SystemConfigController {
    private final SystemConfigRepository repository;
    private final FormRepository formRepository;
    private final GoverConfig goverConfig;

    @Autowired
    public SystemConfigController(SystemConfigRepository repository, FormRepository formRepository, GoverConfig goverConfig) {
        this.repository = repository;
        this.formRepository = formRepository;
        this.goverConfig = goverConfig;
    }

    @GetMapping("/api/system-configs")
    public Collection<SystemConfig> list() {
        return repository.findAll();
    }

    @GetMapping("/api/public/system-configs")
    public Collection<SystemConfig> listPublic() {
        return repository.findSystemConfigsByPublicConfigIsTrue();
    }

    @PutMapping("/api/system-configs/{id}")
    public SystemConfig update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody SystemConfig config
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        var key = SystemConfigKey
                .fromString(id)
                .orElseThrow(NotFoundException::new);

        if (goverConfig.getSystemConfig().containsKey(key.getKey())) {
            throw new ConflictException("Cannot change system config key: " + key.getKey());
        }

        config.setPublicConfig(key.isPublic());

        checkChangingAllowed(config, key);
        performUpdates(config, key);

        return repository
                .findById(id)
                .map(existingConfig -> {
                    existingConfig.setValue(config.getValue());
                    existingConfig.setPublicConfig(config.isPublicConfig());
                    return repository.save(existingConfig);
                })
                .orElseGet(() -> repository.save(config));
    }

    private void checkChangingAllowed(SystemConfig config, SystemConfigKey key) {
        switch (key) {
            case NUTZERKONTEN__BUNDID:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    if (formRepository.existsByStatusAndBundIdEnabledIsTrue(ApplicationStatus.Published)) {
                        throw new ConflictException("Cannot change BundID configuration while published forms are using it");
                    }
                }
                break;
            case NUTZERKONTEN__BAYERN_ID:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    if (formRepository.existsByStatusAndBayernIdEnabledIsTrue(ApplicationStatus.Published)) {
                        throw new ConflictException("Cannot change BayernID configuration while published forms are using it");
                    }
                }
                break;
            case NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    if (formRepository.existsByStatusAndShIdEnabledIsTrue(ApplicationStatus.Published)) {
                        throw new ConflictException("Cannot change SchleswigHolsteinID configuration while published forms are using it");
                    }
                }
                break;
            case NUTZERKONTEN__MUK:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    if (formRepository.existsByStatusAndMukEnabledIsTrue(ApplicationStatus.Published)) {
                        throw new ConflictException("Cannot change MUK configuration while published forms are using it");
                    }
                }
                break;
            default:
                break;
        }
    }

    private void performUpdates(SystemConfig config, SystemConfigKey key) {
        switch (key) {
            case NUTZERKONTEN__BUNDID:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    formRepository
                            .findAllByStatusNotIn(List.of(ApplicationStatus.Published, ApplicationStatus.Revoked))
                            .forEach(form -> {
                                if (form.getBundIdEnabled()) {
                                    form.setBundIdEnabled(false);
                                    form.setBundIdLevel(null);
                                    form.getRoot().setTestProtocolSet(null);
                                    formRepository.save(form);
                                }
                            });
                }
                break;
            case NUTZERKONTEN__BAYERN_ID:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    formRepository
                            .findAllByStatusNotIn(List.of(ApplicationStatus.Published, ApplicationStatus.Revoked))
                            .forEach(form -> {
                                if (form.getBayernIdEnabled()) {
                                    form.setBayernIdEnabled(false);
                                    form.setBayernIdLevel(null);
                                    form.getRoot().setTestProtocolSet(null);
                                    formRepository.save(form);
                                }
                            });
                }
                break;
            case NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    formRepository
                            .findAllByStatusNotIn(List.of(ApplicationStatus.Published, ApplicationStatus.Revoked))
                            .forEach(form -> {
                                if (form.getShIdEnabled()) {
                                    form.setShIdEnabled(false);
                                    form.setShIdLevel(null);
                                    form.getRoot().setTestProtocolSet(null);
                                    formRepository.save(form);
                                }
                            });
                }
                break;
            case NUTZERKONTEN__MUK:
                if (!SystemConfigKey.SYSTEM_CONFIG_TRUE.equalsIgnoreCase(config.getValue())) {
                    formRepository
                            .findAllByStatusNotIn(List.of(ApplicationStatus.Published, ApplicationStatus.Revoked))
                            .forEach(form -> {
                                if (form.getMukEnabled()) {
                                    form.setMukEnabled(false);
                                    form.setMukLevel(null);
                                    form.getRoot().setTestProtocolSet(null);
                                    formRepository.save(form);
                                }
                            });
                }
                break;
            default:
                break;
        }
    }
}

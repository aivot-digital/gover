package de.aivot.GoverBackend.config.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.filters.SystemConfigFilter;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {
    private final SystemConfigRepository configRepository;
    private final SortedMap<String, SystemConfigDefinition> configDefinitions;

    @Autowired
    public SystemConfigService(
            SystemConfigRepository configRepository,
            List<SystemConfigDefinition> configDefinitions
    ) {
        this.configRepository = configRepository;

        // Collect all definitions into a map to make them easier to access later on
        this.configDefinitions = configDefinitions
                .stream()
                .sorted(Comparator.comparing(SystemConfigDefinition::getKey))
                .collect(Collectors.toMap(
                        SystemConfigDefinition::getKey,
                        def -> def,
                        (def1, def2) -> def1,
                        TreeMap::new
                ));
    }

    @Nonnull
    public Page<SystemConfigEntity> list(
            @Nonnull Pageable pageable,
            @Nonnull SystemConfigFilter filter
    ) {
        var allDefinitions = configDefinitions
                .values()
                .stream()
                .filter(def -> (
                        (filter.getPublicConfig() == null || def.isPublicConfig() == filter.getPublicConfig()) &&
                        (filter.getKey() == null || def.getKey().toLowerCase().contains(filter.getKey().toLowerCase()))
                ))
                .toList();

        var entities = configRepository
                .findAll(filter.build())
                .stream()
                .collect(Collectors.toMap(SystemConfigEntity::getKey, entity -> entity));

        if (pageable.getOffset() >= allDefinitions.size()) {
            return Page.empty();
        }

        var sublistStart = pageable.getOffset();
        var sublistEnd = Math.min(allDefinitions.size(), pageable.getOffset() + pageable.getPageSize());

        var slice = allDefinitions
                .subList(
                        (int) sublistStart,
                        (int) sublistEnd
                )
                .stream()
                .map(definition -> {
                    var entity = entities
                            .get(definition.getKey());

                    if (entity != null) {
                        return entity;
                    }

                    String defaultValue;
                    try {
                        defaultValue = definition.serializeValueToDB(definition.getDefaultValue());
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }

                    return new SystemConfigEntity()
                            .setKey(definition.getKey())
                            .setPublicConfig(definition.isPublicConfig())
                            .setValue(defaultValue);
                })
                .toList();

        return new PageImpl<>(slice, pageable, allDefinitions.size());
    }

    @Nonnull
    public SystemConfigEntity retrieve(
            @Nonnull String key
    ) throws ResponseException {
        var def = getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Der Konfigurationsschlüssel \"" + key + "\" ist unbekannt."));

        return configRepository
                .findById(key)
                .orElseGet(() -> {
                    String serializedValue;
                    try {
                        serializedValue = def.serializeValueToDB(def.getDefaultValue());
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }

                    return new SystemConfigEntity()
                            .setKey(key)
                            .setPublicConfig(def.isPublicConfig())
                            .setValue(serializedValue);
                });
    }

    @Nonnull
    public SystemConfigEntity save(
            @Nonnull String key,
            @Nonnull SystemConfigEntity entity
    ) throws ResponseException {
        var definition = getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Der Konfigurationsschlüssel \"" + key + "\" ist unbekannt."));

        definition.validate(entity);

        entity.setKey(key);
        entity.setPublicConfig(definition.isPublicConfig());

        definition.beforeChange(entity);
        configRepository.save(entity);
        definition.afterChange(entity);

        return entity;
    }

    @Nonnull
    public Optional<SystemConfigDefinition> getDefinition(@Nonnull String key) {
        var res = configDefinitions.get(key);
        return Optional.ofNullable(res);
    }
}

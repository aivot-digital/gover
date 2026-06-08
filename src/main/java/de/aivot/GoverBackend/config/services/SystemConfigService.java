package de.aivot.GoverBackend.config.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.filters.SystemConfigFilter;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {
    private final SystemConfigRepository configRepository;
    private final List<SystemConfigDefinition<?>> systemConfigDefinitions;
    private final SortedMap<String, SystemConfigDefinition<?>> configDefinitions;

    @Autowired
    public SystemConfigService(
            SystemConfigRepository configRepository,
            List<SystemConfigDefinition<?>> configDefinitions
    ) {
        this.configRepository = configRepository;

        // Maintain a list of all system config definitions to serve them to the client
        this.systemConfigDefinitions = configDefinitions;

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

        Map<String, SystemConfigEntity> entities = configRepository
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
                .map((SystemConfigDefinition<?> definition) -> {
                    try {
                        return entityOrDefault(definition, entities);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        return new PageImpl<>(slice, pageable, allDefinitions.size());
    }

    private <T> SystemConfigEntity entityOrDefault(@Nonnull SystemConfigDefinition<T> definition,
                                                   @Nonnull Map<String, SystemConfigEntity> entities) throws ResponseException {
        var entity = entities
                .get(definition.getKey());

        if (entity != null) {
            return entity;
        }

        return getDefault(definition);
    }

    @Nonnull
    public SystemConfigEntity retrieve(
            @Nonnull String key
    ) throws ResponseException {
        SystemConfigDefinition<?> def = getDefinition(key)
                .orElseThrow(() -> ResponseException
                        .notFound(
                                "Der Konfigurationsschlüssel %s ist nicht bekannt.",
                                StringUtils.quote(key)
                        ));

        return configRepository
                .findById(key)
                .orElseGet(() -> {
                    try {
                        return getDefault(def);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static <T> SystemConfigEntity getDefault(@Nonnull SystemConfigDefinition<T> def) throws ResponseException {
        String serializedValue = def
                .serializeValueToDB(def.getDefaultValue());

        return new SystemConfigEntity()
                .setKey(def.getKey())
                .setPublicConfig(def.isPublicConfig())
                .setValue(serializedValue);
    }

    @Nonnull
    public SystemConfigEntity save(
            @Nonnull String key,
            @Nonnull SystemConfigEntity entity
    ) throws ResponseException {
        SystemConfigDefinition<?> def = getDefinition(key)
                .orElseThrow(() -> ResponseException
                        .notFound(
                                "Der Konfigurationsschlüssel %s ist nicht bekannt.",
                                StringUtils.quote(key)
                        ));

        validateValue(def, entity.getValue());

        entity.setKey(key);
        entity.setPublicConfig(def.isPublicConfig());

        configRepository.save(entity);

        return entity;
    }

    private <T> void validateValue(@Nonnull SystemConfigDefinition<T> def, @Nullable String val) throws ResponseException {
        T value = def.parseValueFromDB(val);
        def.validate(value);
    }

    @Nonnull
    public Optional<SystemConfigDefinition<?>> getDefinition(@Nonnull String key) {
        SystemConfigDefinition<?> res = configDefinitions.get(key);
        return Optional.ofNullable(res);
    }

    @Nonnull
    public Map<String, Object> getAllConfigsAsMap() throws ResponseException {
        var publicConfigs = configRepository.findAll();

        Map<String, Object> result = new HashMap<>();
        for (var entity : publicConfigs) {
            var def = getDefinition(entity.getKey())
                    .orElseThrow(() -> ResponseException.internalServerError("Unbekannte Systemkonfiguration \"" + entity.getKey() + "\" gefunden."));

            var value = def.parseValueFromDB(entity.getValue());
            result.put(entity.getKey(), value);
        }

        return result;
    }

    @Nonnull
    public Map<String, Object> getPublicConfigsAsMap() throws ResponseException {
        var publicConfigs = configRepository.findAll(
                SystemConfigFilter
                        .create()
                        .setPublicConfig(true)
                        .build()
        );

        Map<String, Object> result = new HashMap<>();
        for (var entity : publicConfigs) {
            var def = getDefinition(entity.getKey())
                    .orElseThrow(() -> ResponseException.internalServerError("Unbekannte Systemkonfiguration \"" + entity.getKey() + "\" gefunden."));

            var value = def.parseValueFromDB(entity.getValue());
            result.put(entity.getKey(), value);
        }

        return result;
    }

    public List<SystemConfigDefinition<?>> getSystemConfigDefinitions() {
        return systemConfigDefinitions;
    }
}

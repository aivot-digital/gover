package de.aivot.prosuna.backend.config.services;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.filters.SystemConfigFilter;
import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.utils.StringUtils;
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

    @Nullable
    public Object getValue(@Nonnull String key) throws ResponseException {
        var def = getDefinition(key)
                .orElseThrow(() -> ResponseException
                        .notFound(
                                "Der Konfigurationsschlüssel %s ist nicht bekannt.",
                                StringUtils.quote(key)
                        ));

        return getValue(def);
    }

    @Nullable
    private <T> T getValue(@Nonnull SystemConfigDefinition<T> def) throws ResponseException {
        var entity = configRepository
                .findById(def.getKey());

        if (entity.isEmpty()) {
            return parseDefaultValue(def);
        }

        return def.parseValueFromDB(entity.get().getValue());
    }

    @Nullable
    private static <T> T parseDefaultValue(@Nonnull SystemConfigDefinition<T> def) throws ResponseException {
        String serializedValue = def
                .serializeValueToDB(def.getDefaultValue());

        return def.parseValueFromDB(serializedValue);
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
        return save(key, entity, false);
    }

    @Nonnull
    public SystemConfigEntity save(
            @Nonnull String key,
            @Nonnull SystemConfigEntity entity,
            boolean changeConfirmed
    ) throws ResponseException {
        SystemConfigDefinition<?> def = getDefinition(key)
                .orElseThrow(() -> ResponseException
                        .notFound(
                                "Der Konfigurationsschlüssel %s ist nicht bekannt.",
                                StringUtils.quote(key)
                        ));

        var oldEntity = configRepository
                .findById(key)
                .orElseGet(() -> {
                    try {
                        return getDefault(def);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }
                });

        validateChange(def, oldEntity.getValue(), entity.getValue(), changeConfirmed);

        entity.setKey(key);
        entity.setPublicConfig(def.isPublicConfig());

        configRepository.save(entity);

        return entity;
    }

    private <T> void validateChange(@Nonnull SystemConfigDefinition<T> def,
                                    @Nonnull String oldVal,
                                    @Nonnull String newVal,
                                    boolean changeConfirmed) throws ResponseException {
        T oldValue = def.parseValueFromDB(oldVal);
        T newValue = def.parseValueFromDB(newVal);
        def.validate(newValue);
        def.validateChange(oldValue, newValue, changeConfirmed);
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

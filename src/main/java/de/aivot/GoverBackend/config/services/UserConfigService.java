package de.aivot.GoverBackend.config.services;

import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.entities.UserConfigEntityId;
import de.aivot.GoverBackend.config.filters.UserConfigFilter;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.config.repositories.UserConfigRepository;
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
public class UserConfigService {
    private final UserConfigRepository configRepository;
    private final SortedMap<String, UserConfigDefinition> configDefinitions;

    @Autowired
    public UserConfigService(
            UserConfigRepository configRepository,
            List<UserConfigDefinition> configDefinitions
    ) {
        this.configRepository = configRepository;

        // Collect all definitions into a map to make them easier to access later on
        this.configDefinitions = configDefinitions
                .stream()
                .sorted(Comparator.comparing(UserConfigDefinition::getKey))
                .collect(Collectors.toMap(
                        UserConfigDefinition::getKey,
                        def -> def,
                        (def1, def2) -> def1,
                        TreeMap::new
                ));
    }

    @Nonnull
    public Page<UserConfigEntity> list(
            @Nonnull Pageable pageable,
            @Nonnull UserConfigFilter filter
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
                .collect(Collectors.toMap(UserConfigEntity::getKey, entity -> entity));

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

                    return new UserConfigEntity()
                            .setKey(definition.getKey())
                            .setPublicConfig(definition.isPublicConfig())
                            .setValue(defaultValue);
                })
                .toList();

        return new PageImpl<>(slice, pageable, allDefinitions.size());
    }

    @Nonnull
    public UserConfigEntity retrieve(
            @Nonnull String key,
            @Nonnull String userId
    ) throws ResponseException {
        var def = getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Der Konfigurationsschlüssel \"" + key + "\" ist unbekannt."));

        var id = new UserConfigEntityId(userId, key);

        return configRepository
                .findById(id)
                .orElseGet(() -> {
                    String serializedValue;
                    try {
                        serializedValue = def.serializeValueToDB(def.getDefaultValue());
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }

                    return new UserConfigEntity()
                            .setKey(key)
                            .setPublicConfig(def.isPublicConfig())
                            .setValue(serializedValue);
                });
    }

    @Nonnull
    public UserConfigEntity save(
            @Nonnull String key,
            @Nonnull String userId,
            @Nonnull UserConfigEntity entity
    ) throws ResponseException {
        var definition = getDefinition(key)
                .orElseThrow(() -> ResponseException.notFound("Der Konfigurationsschlüssel \"" + key + "\" ist unbekannt."));

        definition.validate(entity);

        entity.setKey(key);
        entity.setUserId(userId);
        entity.setPublicConfig(definition.isPublicConfig());

        definition.beforeChange(entity);
        configRepository.save(entity);
        definition.afterChange(entity);

        return entity;
    }

    @Nonnull
    public Optional<UserConfigDefinition> getDefinition(@Nonnull String key) {
        var res = configDefinitions.get(key);
        return Optional.ofNullable(res);
    }
}

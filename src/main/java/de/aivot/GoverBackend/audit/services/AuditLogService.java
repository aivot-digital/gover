package de.aivot.GoverBackend.audit.services;

import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import de.aivot.GoverBackend.audit.filters.AuditLogFilter;
import de.aivot.GoverBackend.audit.models.AuditLogFilterActorOption;
import de.aivot.GoverBackend.audit.models.AuditLogFilterOptions;
import de.aivot.GoverBackend.audit.repositories.AuditLogRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.CreateEntityService;
import de.aivot.GoverBackend.lib.services.ListEntityService;
import de.aivot.GoverBackend.lib.services.RetrieveEntityService;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogService implements CreateEntityService<AuditLogEntity>, ListEntityService<AuditLogEntity>, RetrieveEntityService<AuditLogEntity, Long> {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Nonnull
    @Override
    public AuditLogEntity create(@Nonnull AuditLogEntity entity) throws ResponseException {
        var now = LocalDateTime.now();

        entity.setId(null);

        if (entity.getTimestamp() == null) {
            entity.setTimestamp(now);
        }
        if (entity.getMetadata() == null) {
            entity.setMetadata(Map.of());
        }
        if (entity.getActorType() == null) {
            entity.setActorType("System");
        }
        if (entity.getTriggerType() == null) {
            entity.setTriggerType("Message");
        }
        if (entity.getModule() == null) {
            entity.setModule("General");
        }
        if (entity.getMessage() == null) {
            entity.setMessage("Audit event");
        }

        return auditLogRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<AuditLogEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<AuditLogEntity> specification,
            @Nullable Filter<AuditLogEntity> filter
    ) throws ResponseException {
        if (filter instanceof AuditLogFilter auditFilter) {
            resolveActorFilters(auditFilter);
            specification = auditFilter.build();
        }

        if (specification == null) {
            return auditLogRepository.findAll(pageable);
        }

        return auditLogRepository.findAll(specification, pageable);
    }

    @Nonnull
    public AuditLogFilterOptions getFilterOptions() {
        var modules = auditLogRepository.findDistinctModules();
        var triggerTypes = auditLogRepository.findDistinctTriggerTypes();
        var actorIds = auditLogRepository.findDistinctActorIds();

        var usersById = userRepository.findAllById(actorIds)
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getId().trim(),
                        entry -> entry.getFullName().trim(),
                        (first, second) -> first
                ));

        var actorOptions = actorIds
                .stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(actorId -> new AuditLogFilterActorOption(
                        actorId,
                        usersById.containsKey(actorId) ? usersById.get(actorId) : actorId
                ))
                .toList();

        return new AuditLogFilterOptions(modules, triggerTypes, actorOptions);
    }

    @Nonnull
    @Override
    public Optional<AuditLogEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return auditLogRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<AuditLogEntity> retrieve(@Nonnull Specification<AuditLogEntity> specification) throws ResponseException {
        return auditLogRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return auditLogRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<AuditLogEntity> specification) {
        return auditLogRepository.exists(specification);
    }

    private void resolveActorFilters(@Nonnull AuditLogFilter filter) {
        var currentActors = normalizeValues(filter.getActors());
        if (currentActors.isEmpty()) {
            return;
        }

        var resolvedActorIds = new LinkedHashSet<>(currentActors);
        for (var actor : currentActors) {
            var matchingUserIds = userRepository.findIdsByFullNameContaining(actor);
            resolvedActorIds.addAll(normalizeValues(matchingUserIds));
        }

        filter.setActors(List.copyOf(resolvedActorIds));
    }

    @Nonnull
    private static List<String> normalizeValues(@Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }
}

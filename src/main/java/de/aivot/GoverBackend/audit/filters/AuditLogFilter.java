package de.aivot.GoverBackend.audit.filters;

import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AuditLogFilter implements Filter<AuditLogEntity> {
    private Long id;
    private String actorType;
    private String actorId;
    private List<String> actors;
    private String triggerType;
    private List<String> triggerTypes;
    private String entityRef;
    private String entityRefType;
    private String module;
    private List<String> modules;
    private String ipAddress;
    private LocalDateTime timestampFrom;
    private LocalDateTime timestampTo;

    public static AuditLogFilter create() {
        return new AuditLogFilter();
    }

    @Nonnull
    @Override
    public Specification<AuditLogEntity> build() {
        var builder = SpecificationBuilder
                .create(AuditLogEntity.class)
                .withEquals("id", id)
                .withContains("actorType", actorType)
                .withContains("actorId", actorId)
                .withContains("triggerType", triggerType)
                .withContains("entityRef", entityRef)
                .withContains("entityRefType", entityRefType)
                .withContains("module", module)
                .withInList("triggerType", normalizeValues(triggerTypes))
                .withInList("module", normalizeValues(modules))
                .withContains("ipAddress", ipAddress);

        var normalizedActors = normalizeValues(actors);
        if (!normalizedActors.isEmpty()) {
            builder.withSpecification((root, query, specBuilder) -> specBuilder.or(
                    normalizedActors
                            .stream()
                            .map(entry -> specBuilder.like(specBuilder.lower(root.get("actorId")), "%" + entry.toLowerCase(Locale.ROOT) + "%"))
                            .toArray(jakarta.persistence.criteria.Predicate[]::new)
            ));
        }

        if (timestampFrom != null) {
            builder.withSpecification((root, query, specBuilder) ->
                    specBuilder.greaterThanOrEqualTo(root.get("timestamp"), timestampFrom)
            );
        }

        if (timestampTo != null) {
            builder.withSpecification((root, query, specBuilder) ->
                    specBuilder.lessThanOrEqualTo(root.get("timestamp"), timestampTo)
            );
        }

        return builder.build();
    }

    @Nullable
    public Long getId() {
        return id;
    }

    public AuditLogFilter setId(@Nullable Long id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getActorType() {
        return actorType;
    }

    public AuditLogFilter setActorType(@Nullable String actorType) {
        this.actorType = actorType;
        return this;
    }

    @Nullable
    public String getActorId() {
        return actorId;
    }

    public AuditLogFilter setActorId(@Nullable String actorId) {
        this.actorId = actorId;
        return this;
    }

    @Nullable
    public List<String> getActors() {
        return actors;
    }

    public AuditLogFilter setActors(@Nullable List<String> actors) {
        this.actors = actors;
        return this;
    }

    @Nullable
    public String getTriggerType() {
        return triggerType;
    }

    public AuditLogFilter setTriggerType(@Nullable String triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    @Nullable
    public List<String> getTriggerTypes() {
        return triggerTypes;
    }

    public AuditLogFilter setTriggerTypes(@Nullable List<String> triggerTypes) {
        this.triggerTypes = triggerTypes;
        return this;
    }

    @Nullable
    public String getEntityRef() {
        return entityRef;
    }

    public AuditLogFilter setEntityRef(@Nullable String entityRef) {
        this.entityRef = entityRef;
        return this;
    }

    @Nullable
    public String getEntityRefType() {
        return entityRefType;
    }

    public AuditLogFilter setEntityRefType(@Nullable String entityRefType) {
        this.entityRefType = entityRefType;
        return this;
    }

    @Nullable
    public String getTriggerRef() {
        return entityRef;
    }

    public AuditLogFilter setTriggerRef(@Nullable String triggerRef) {
        this.entityRef = triggerRef;
        return this;
    }

    @Nullable
    public String getTriggerRefType() {
        return entityRefType;
    }

    public AuditLogFilter setTriggerRefType(@Nullable String triggerRefType) {
        this.entityRefType = triggerRefType;
        return this;
    }

    @Nullable
    public String getModule() {
        return module;
    }

    public AuditLogFilter setModule(@Nullable String module) {
        this.module = module;
        return this;
    }

    @Nullable
    public List<String> getModules() {
        return modules;
    }

    public AuditLogFilter setModules(@Nullable List<String> modules) {
        this.modules = modules;
        return this;
    }

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    public AuditLogFilter setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    @Nullable
    public LocalDateTime getTimestampFrom() {
        return timestampFrom;
    }

    public AuditLogFilter setTimestampFrom(@Nullable LocalDateTime timestampFrom) {
        this.timestampFrom = timestampFrom;
        return this;
    }

    @Nullable
    public LocalDateTime getTimestampTo() {
        return timestampTo;
    }

    public AuditLogFilter setTimestampTo(@Nullable LocalDateTime timestampTo) {
        this.timestampTo = timestampTo;
        return this;
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

package de.aivot.GoverBackend.user.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import de.aivot.GoverBackend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class VUserDeputyWithDetailsFilter implements Filter<VUserDeputyWithDetailsEntity> {
    private String originalUserId;
    private String originalUserFullName;
    private String deputyUserId;
    private String deputyUserFullName;
    private LocalDateTime fromTimestamp;
    private Boolean untilTimestampIsNull;

    public static VUserDeputyWithDetailsFilter create() {
        return new VUserDeputyWithDetailsFilter();
    }

    @Override
    public Specification<VUserDeputyWithDetailsEntity> build() {
        var builder = SpecificationBuilder
                .create(VUserDeputyWithDetailsEntity.class)
                .withEquals("originalUserId", originalUserId)
                .withEquals("deputyUserId", deputyUserId)
                .withContains("originalUserFullName", originalUserFullName)
                .withContains("deputyUserFullName", deputyUserFullName);

        if (fromTimestamp != null) {
            builder = builder
                    .withGreaterThan("fromTimestamp", fromTimestamp.atZone(ZoneId.systemDefault()).toEpochSecond());
        }

        if (Boolean.TRUE.equals(untilTimestampIsNull)) {
            builder = builder
                    .withNull("untilTimestamp");
        }

        return builder.build();
    }

    public String getOriginalUserId() {
        return originalUserId;
    }

    public VUserDeputyWithDetailsFilter setOriginalUserId(String originalUserId) {
        this.originalUserId = originalUserId;
        return this;
    }

    public String getOriginalUserFullName() {
        return originalUserFullName;
    }

    public VUserDeputyWithDetailsFilter setOriginalUserFullName(String originalUserFullName) {
        this.originalUserFullName = originalUserFullName;
        return this;
    }

    public String getDeputyUserId() {
        return deputyUserId;
    }

    public VUserDeputyWithDetailsFilter setDeputyUserId(String deputyUserId) {
        this.deputyUserId = deputyUserId;
        return this;
    }

    public String getDeputyUserFullName() {
        return deputyUserFullName;
    }

    public VUserDeputyWithDetailsFilter setDeputyUserFullName(String deputyUserFullName) {
        this.deputyUserFullName = deputyUserFullName;
        return this;
    }

    public LocalDateTime getFromTimestamp() {
        return fromTimestamp;
    }

    public VUserDeputyWithDetailsFilter setFromTimestamp(LocalDateTime fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
        return this;
    }

    public Boolean getUntilTimestampIsNull() {
        return untilTimestampIsNull;
    }

    public VUserDeputyWithDetailsFilter setUntilTimestampIsNull(Boolean untilTimestampIsNull) {
        this.untilTimestampIsNull = untilTimestampIsNull;
        return this;
    }
}

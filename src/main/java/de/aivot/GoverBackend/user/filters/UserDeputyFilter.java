package de.aivot.GoverBackend.user.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class UserDeputyFilter implements Filter<UserDeputyEntity> {
    private String originalUserId;
    private String deputyUserId;
    private LocalDateTime fromTimestamp;
    private Boolean untilTimestampIsNull;

    public static UserDeputyFilter create() {
        return new UserDeputyFilter();
    }

    @Override
    public Specification<UserDeputyEntity> build() {
        var builder = SpecificationBuilder
                .create(UserDeputyEntity.class)
                .withEquals("originalUserId", originalUserId)
                .withEquals("deputyUserId", deputyUserId);

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

    public UserDeputyFilter setOriginalUserId(String originalUserId) {
        this.originalUserId = originalUserId;
        return this;
    }

    public String getDeputyUserId() {
        return deputyUserId;
    }

    public UserDeputyFilter setDeputyUserId(String deputyUserId) {
        this.deputyUserId = deputyUserId;
        return this;
    }

    public LocalDateTime getFromTimestamp() {
        return fromTimestamp;
    }

    public UserDeputyFilter setFromTimestamp(LocalDateTime fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
        return this;
    }

    public Boolean getUntilTimestampIsNull() {
        return untilTimestampIsNull;
    }

    public UserDeputyFilter setUntilTimestampIsNull(Boolean untilTimestampIsNull) {
        this.untilTimestampIsNull = untilTimestampIsNull;
        return this;
    }
}

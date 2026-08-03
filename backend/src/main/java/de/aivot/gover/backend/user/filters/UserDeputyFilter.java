package de.aivot.gover.backend.user.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.user.entities.UserDeputyEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class UserDeputyFilter implements Filter<UserDeputyEntity> {
    private String originalUserId;
    private String deputyUserId;
    private LocalDate fromDate;
    private Boolean untilDateIsNull;

    public static UserDeputyFilter create() {
        return new UserDeputyFilter();
    }

    @Override
    public Specification<UserDeputyEntity> build() {
        var builder = SpecificationBuilder
                .create(UserDeputyEntity.class)
                .withEquals("originalUserId", originalUserId)
                .withEquals("deputyUserId", deputyUserId);

        if (fromDate != null) {
            builder = builder
                    .withSpecification((root, query, criteriaBuilder) ->
                            criteriaBuilder.greaterThan(root.get("fromDate"), fromDate)
                    );
        }

        if (Boolean.TRUE.equals(untilDateIsNull)) {
            builder = builder
                    .withNull("untilDate");
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

    public LocalDate getFromDate() {
        return fromDate;
    }

    public UserDeputyFilter setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public Boolean getUntilDateIsNull() {
        return untilDateIsNull;
    }

    public UserDeputyFilter setUntilDateIsNull(Boolean untilDateIsNull) {
        this.untilDateIsNull = untilDateIsNull;
        return this;
    }
}

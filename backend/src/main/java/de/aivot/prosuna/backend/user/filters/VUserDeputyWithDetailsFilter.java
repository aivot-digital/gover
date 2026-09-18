package de.aivot.prosuna.backend.user.filters;

import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class VUserDeputyWithDetailsFilter implements Filter<VUserDeputyWithDetailsEntity> {
    private String originalUserId;
    private String originalUserFullName;
    private String deputyUserId;
    private String deputyUserFullName;
    private LocalDate fromDate;
    private Boolean untilDateIsNull;

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

    public LocalDate getFromDate() {
        return fromDate;
    }

    public VUserDeputyWithDetailsFilter setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public Boolean getUntilDateIsNull() {
        return untilDateIsNull;
    }

    public VUserDeputyWithDetailsFilter setUntilDateIsNull(Boolean untilDateIsNull) {
        this.untilDateIsNull = untilDateIsNull;
        return this;
    }
}

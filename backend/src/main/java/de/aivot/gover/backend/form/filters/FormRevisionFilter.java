package de.aivot.gover.backend.form.filters;

import de.aivot.gover.backend.form.entities.FormRevisionEntity;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class FormRevisionFilter implements Filter<FormRevisionEntity> {
    private Integer formId;
    private Integer formVersion;
    private String userId;

    public static FormRevisionFilter create() {
        return new FormRevisionFilter();
    }

    @Nonnull
    @Override
    public Specification<FormRevisionEntity> build() {
        return SpecificationBuilder
                .create(FormRevisionEntity.class)
                .withEquals("formId", formId)
                .withEquals("formVersion", formVersion)
                .withEquals("userId", userId)
                .build();
    }

    public Integer getFormId() {
        return formId;
    }

    public FormRevisionFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getFormVersion() {
        return formVersion;
    }

    public FormRevisionFilter setFormVersion(Integer formVersion) {
        this.formVersion = formVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormRevisionFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}

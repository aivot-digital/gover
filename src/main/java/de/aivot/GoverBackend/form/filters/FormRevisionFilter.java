package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormRevision;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class FormRevisionFilter implements Filter<FormRevision> {
    private Integer formId;
    private String userId;

    public static FormRevisionFilter create() {
        return new FormRevisionFilter();
    }

    public FormRevisionFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public FormRevisionFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    @Override
    public Specification<FormRevision> build() {
        return SpecificationBuilder
                .create(FormRevision.class)
                .withEquals("formId", formId)
                .withEquals("userId", userId)
                .build();
    }
}

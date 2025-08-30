package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormRevision;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class FormSlugHistoryFilter implements Filter<FormRevision> {
    private String slug;
    private Integer formId;

    public static FormSlugHistoryFilter create() {
        return new FormSlugHistoryFilter();
    }

    @Nonnull
    @Override
    public Specification<FormRevision> build() {
        return SpecificationBuilder
                .create(FormRevision.class)
                .withEquals("slug", slug)
                .withEquals("formId", formId)
                .build();
    }

    public String getSlug() {
        return slug;
    }

    public FormSlugHistoryFilter setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public FormSlugHistoryFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }
}

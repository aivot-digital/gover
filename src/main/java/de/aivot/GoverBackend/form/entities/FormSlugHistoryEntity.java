package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "form_slug_history")
public class FormSlugHistoryEntity {
    @Id
    @Nonnull
    private String slug;

    @Nonnull
    private Integer formId;

    // Default constructor for JPA
    public FormSlugHistoryEntity() {
        slug = "";
        formId = 0;
    }

    // Constructor with parameters
    public FormSlugHistoryEntity(@Nonnull String slug, @Nonnull Integer formId) {
        this.slug = slug;
        this.formId = formId;
    }

    @Nonnull
    public String getSlug() {
        return slug;
    }

    public FormSlugHistoryEntity setSlug(@Nonnull String slug) {
        this.slug = slug;
        return this;
    }

    @Nonnull
    public Integer getFormId() {
        return formId;
    }

    public FormSlugHistoryEntity setFormId(@Nonnull Integer formId) {
        this.formId = formId;
        return this;
    }
}

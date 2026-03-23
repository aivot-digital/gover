package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;

public class FormVersionEntityId {
    @Nonnull
    private Integer formId;
    @Nonnull
    private Integer version;

    // Default constructor for JPA
    public FormVersionEntityId() {
        this.formId = 0;
        this.version = 0;
    }

    // Constructor with parameters
    public FormVersionEntityId(@Nonnull Integer formId, @Nonnull Integer version) {
        this.formId = formId;
        this.version = version;
    }

    // Static factory method
    public static FormVersionEntityId of(@Nonnull Integer formId, @Nonnull Integer version) {
        return new FormVersionEntityId(formId, version);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormVersionEntityId that = (FormVersionEntityId) object;
        return formId.equals(that.formId) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = formId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Nonnull
    public Integer getFormId() {
        return formId;
    }

    public FormVersionEntityId setFormId(@Nonnull Integer formId) {
        this.formId = formId;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public FormVersionEntityId setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }
}

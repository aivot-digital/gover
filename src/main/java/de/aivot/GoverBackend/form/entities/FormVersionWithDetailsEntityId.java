package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;

public class FormVersionWithDetailsEntityId {
    @Nonnull
    private Integer id;
    @Nonnull
    private Integer version;

    public FormVersionWithDetailsEntityId() {
        this.id = 0;
        this.version = 0;
    }

    public FormVersionWithDetailsEntityId(@Nonnull Integer id, @Nonnull Integer version) {
        this.id = id;
        this.version = version;
    }

    @Nonnull
    public static FormVersionWithDetailsEntityId of(@Nonnull Integer id, @Nonnull Integer version) {
        return new FormVersionWithDetailsEntityId(id, version);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormVersionWithDetailsEntityId that = (FormVersionWithDetailsEntityId) object;
        return id.equals(that.id) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public FormVersionWithDetailsEntityId setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public FormVersionWithDetailsEntityId setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }

    @Override
    public String toString() {
        return "FormVersionWithDetailsEntityId{" +
               "id=" + id +
               ", version=" + version +
               '}';
    }
}

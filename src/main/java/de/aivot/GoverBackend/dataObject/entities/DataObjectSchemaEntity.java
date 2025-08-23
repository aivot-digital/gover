package de.aivot.GoverBackend.dataObject.entities;

import de.aivot.GoverBackend.core.converters.GroupLayoutConverter;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_object_schemas")
public class DataObjectSchemaEntity {
    @Id
    @Nonnull
    @Column(length = 64)
    private String key;

    @Nonnull
    @Column(length = 96)
    private String name;

    @Nonnull
    @Column(columnDefinition = "text")
    private String description;

    @Nonnull
    @Column(length = 64)
    private String idGen;

    @Nonnull
    @Convert(converter = GroupLayoutConverter.class)
    @Column(columnDefinition = "jsonb")
    private GroupLayout schema;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // region Equals & Hash

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        DataObjectSchemaEntity that = (DataObjectSchemaEntity) o;
        return key.equals(that.key) && name.equals(that.name) && description.equals(that.description) && idGen.equals(that.idGen) && schema.equals(that.schema) && created.equals(that.created) && updated.equals(that.updated);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + idGen.hashCode();
        result = 31 * result + schema.hashCode();
        result = 31 * result + created.hashCode();
        result = 31 * result + updated.hashCode();
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public String getKey() {
        return key;
    }

    public DataObjectSchemaEntity setKey(@Nonnull String key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public DataObjectSchemaEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public DataObjectSchemaEntity setDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public String getIdGen() {
        return idGen;
    }

    public DataObjectSchemaEntity setIdGen(@Nonnull String idGen) {
        this.idGen = idGen;
        return this;
    }

    @Nonnull
    public GroupLayout getSchema() {
        return schema;
    }

    public DataObjectSchemaEntity setSchema(@Nonnull GroupLayout schema) {
        this.schema = schema;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public DataObjectSchemaEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public DataObjectSchemaEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}

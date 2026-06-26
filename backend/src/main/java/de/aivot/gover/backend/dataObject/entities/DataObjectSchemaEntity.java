package de.aivot.gover.backend.dataObject.entities;

import de.aivot.gover.backend.core.converters.GroupLayoutConverter;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

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
    private GroupLayoutElement schema;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    @Nonnull
    @Column(columnDefinition = "varchar(64)[]")
    private List<String> displayFields;

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    // region Equals & Hash

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataObjectSchemaEntity that = (DataObjectSchemaEntity) o;
        return Objects.equals(key, that.key) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(idGen, that.idGen) &&
                Objects.equals(schema, that.schema) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated) &&
                Objects.equals(displayFields, that.displayFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, idGen, schema, created, updated, displayFields);
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
    public GroupLayoutElement getSchema() {
        return schema;
    }

    public DataObjectSchemaEntity setSchema(@Nonnull GroupLayoutElement schema) {
        this.schema = schema;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public DataObjectSchemaEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public DataObjectSchemaEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public List<String> getDisplayFields() {
        return displayFields;
    }

    public DataObjectSchemaEntity setDisplayFields(@Nonnull List<String> displayFields) {
        this.displayFields = displayFields;
        return this;
    }

    // endregion
}

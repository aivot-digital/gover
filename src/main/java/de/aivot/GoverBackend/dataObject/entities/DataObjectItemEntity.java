package de.aivot.GoverBackend.dataObject.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "data_object_items")
@IdClass(DataObjectItemEntityId.class)
public class DataObjectItemEntity {
    @Id
    @Nonnull
    @Column(length = 64)
    private String schemaKey;

    @Id
    @Nonnull
    @Column(length = 64)
    private String id;

    @Nonnull
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

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

        DataObjectItemEntity that = (DataObjectItemEntity) o;
        return schemaKey.equals(that.schemaKey) && id.equals(that.id) && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        int result = schemaKey.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + data.hashCode();
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public String getSchemaKey() {
        return schemaKey;
    }

    public DataObjectItemEntity setSchemaKey(@Nonnull String schemaKey) {
        this.schemaKey = schemaKey;
        return this;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public DataObjectItemEntity setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Map<String, Object> getData() {
        return data;
    }

    public DataObjectItemEntity setData(@Nonnull Map<String, Object> data) {
        this.data = data;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public DataObjectItemEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public DataObjectItemEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}

package de.aivot.gover.backend.codeLists.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "v_code_list_items")
public class VCodeListItemEntity {
    @Id
    @Nonnull
    private Long id;

    @Nonnull
    private Integer codeListId;

    @Nonnull
    @Column(columnDefinition = "varchar(96)[]")
    private List<String> columns;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    @Nonnull
    private String value;

    @Nonnull
    private String label;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VCodeListItemEntity that = (VCodeListItemEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(codeListId, that.codeListId) && Objects.equals(columns, that.columns) &&
                Objects.equals(created, that.created) && Objects.equals(updated, that.updated) && Objects.equals(value, that.value) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codeListId, columns, created, updated, value, label);
    }

    @Nonnull
    public Long getId() {
        return id;
    }

    public VCodeListItemEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getCodeListId() {
        return codeListId;
    }

    public VCodeListItemEntity setCodeListId(@Nonnull Integer codeListId) {
        this.codeListId = codeListId;
        return this;
    }

    @Nonnull
    public List<String> getColumns() {
        return columns;
    }

    public VCodeListItemEntity setColumns(@Nonnull List<String> columns) {
        this.columns = columns;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public VCodeListItemEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public VCodeListItemEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    public VCodeListItemEntity setValue(@Nonnull String value) {
        this.value = value;
        return this;
    }

    @Nonnull
    public String getLabel() {
        return label;
    }

    public VCodeListItemEntity setLabel(@Nonnull String label) {
        this.label = label;
        return this;
    }
}

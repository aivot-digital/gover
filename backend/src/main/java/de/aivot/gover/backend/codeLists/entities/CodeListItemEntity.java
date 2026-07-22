package de.aivot.gover.backend.codeLists.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "code_list_items")
public class CodeListItemEntity {
    private static final String ID_SEQUENCE_NAME = "code_list_items_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
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

    @PrePersist
    public void prePersist() {
        var now = Instant.now();
        created = now;
        updated = now;
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CodeListItemEntity that = (CodeListItemEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(codeListId, that.codeListId) && Objects.equals(columns, that.columns) &&
                Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codeListId, columns, created, updated);
    }

    @Nonnull
    public Long getId() {
        return id;
    }

    public CodeListItemEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getCodeListId() {
        return codeListId;
    }

    public CodeListItemEntity setCodeListId(@Nonnull Integer codeListId) {
        this.codeListId = codeListId;
        return this;
    }

    @Nonnull
    public List<String> getColumns() {
        return columns;
    }

    public CodeListItemEntity setColumns(@Nonnull List<String> columns) {
        this.columns = columns;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public CodeListItemEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public CodeListItemEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }
}

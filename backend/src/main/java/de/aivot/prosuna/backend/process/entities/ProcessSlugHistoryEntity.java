package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "process_slug_history")
public class ProcessSlugHistoryEntity {
    @Id
    @Nonnull
    private String slug;

    @Nonnull
    private Integer processId;

    @Nonnull
    private Instant created;

    public ProcessSlugHistoryEntity() {
        slug = "";
        processId = 0;
        created = Instant.now();
    }

    public ProcessSlugHistoryEntity(@Nonnull String slug, @Nonnull Integer processId) {
        this.slug = slug;
        this.processId = processId;
        created = Instant.now();
    }

    public ProcessSlugHistoryEntity(@Nonnull String slug, @Nonnull Integer processId, @Nonnull Instant created) {
        this.slug = slug;
        this.processId = processId;
        this.created = created;
    }

    @Nonnull
    public String getSlug() {
        return slug;
    }

    public ProcessSlugHistoryEntity setSlug(@Nonnull String slug) {
        this.slug = slug;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessSlugHistoryEntity setProcessId(@Nonnull Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public ProcessSlugHistoryEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }
}

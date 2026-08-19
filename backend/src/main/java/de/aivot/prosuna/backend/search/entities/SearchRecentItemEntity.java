package de.aivot.prosuna.backend.search.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "search_recent_items")
public class SearchRecentItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Nonnull
    @Column(name = "origin_table", length = 64, nullable = false)
    private String originTable;

    @Nonnull
    @Column(name = "item_id", columnDefinition = "TEXT", nullable = false)
    private String itemId;

    @Nonnull
    @Column(nullable = false)
    private Instant created;

    @Nonnull
    @Column(name = "last_accessed", nullable = false)
    private Instant lastAccessed;

    public SearchRecentItemEntity() {
        userId = "";
        originTable = "";
        itemId = "";
        created = Instant.now();
        lastAccessed = created;
    }

    @PrePersist
    public void prePersist() {
        if (created == null) {
            created = Instant.now();
        }
        if (lastAccessed == null) {
            lastAccessed = created;
        }
    }

    public Long getId() {
        return id;
    }

    public SearchRecentItemEntity setId(Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public SearchRecentItemEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public String getOriginTable() {
        return originTable;
    }

    public SearchRecentItemEntity setOriginTable(@Nonnull String originTable) {
        this.originTable = originTable;
        return this;
    }

    @Nonnull
    public String getItemId() {
        return itemId;
    }

    public SearchRecentItemEntity setItemId(@Nonnull String itemId) {
        this.itemId = itemId;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public SearchRecentItemEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getLastAccessed() {
        return lastAccessed;
    }

    public SearchRecentItemEntity setLastAccessed(@Nonnull Instant lastAccessed) {
        this.lastAccessed = lastAccessed;
        return this;
    }
}

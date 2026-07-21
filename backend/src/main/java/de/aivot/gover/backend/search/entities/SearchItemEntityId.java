package de.aivot.gover.backend.search.entities;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

public class SearchItemEntityId implements Serializable {
    @Nonnull
    private String id;

    @Nonnull
    private String originTable;

    @Nonnull
    private String userId;

    public SearchItemEntityId() {
        id = "";
        originTable = "";
        userId = "";
    }

    public SearchItemEntityId(@Nonnull String id,
                              @Nonnull String originTable,
                              @Nonnull String userId) {
        this.id = id;
        this.originTable = originTable;
        this.userId = userId;
    }

    public static SearchItemEntityId of(@Nonnull String id,
                                        @Nonnull String originTable,
                                        @Nonnull String userId) {
        return new SearchItemEntityId(id, originTable, userId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchItemEntityId that = (SearchItemEntityId) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(originTable, that.originTable) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, originTable, userId);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public SearchItemEntityId setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getOriginTable() {
        return originTable;
    }

    public SearchItemEntityId setOriginTable(@Nonnull String originTable) {
        this.originTable = originTable;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public SearchItemEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }
}

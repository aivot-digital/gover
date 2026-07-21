package de.aivot.gover.backend.search.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "v_search_items")
@IdClass(SearchItemEntityId.class)
public class SearchItemEntity {
    @Id
    private String id;

    private String label;

    @Id
    private String originTable;

    private String originTableSubset;

    // SearchFilter needs this view-only column for word_similarity; no getter keeps it out of API responses.
    @Column(name = "search_text")
    private String searchText;

    @Id
    private String userId;

    private List<String> permissions;

    public String getId() {
        return id;
    }

    public SearchItemEntity setId(String id) {
        this.id = id;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public SearchItemEntity setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getOriginTable() {
        return originTable;
    }

    public SearchItemEntity setOriginTable(String originTable) {
        this.originTable = originTable;
        return this;
    }

    public String getOriginTableSubset() {
        return originTableSubset;
    }

    public SearchItemEntity setOriginTableSubset(String originTableSubset) {
        this.originTableSubset = originTableSubset;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public SearchItemEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public SearchItemEntity setPermissions(List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}

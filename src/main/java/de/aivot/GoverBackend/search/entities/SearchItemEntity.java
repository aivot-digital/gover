package de.aivot.GoverBackend.search.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "v_search_items")
public class SearchItemEntity {
    @Id
    private String id;

    private String label;

    private String originTable;

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
}

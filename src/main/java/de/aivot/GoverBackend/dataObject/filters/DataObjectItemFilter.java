package de.aivot.GoverBackend.dataObject.filters;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class DataObjectItemFilter implements Filter<DataObjectItemEntity> {
    private String schemaKey;
    private String id;

    public static DataObjectItemFilter create() {
        return new DataObjectItemFilter();
    }

    @NotNull
    @Override
    public Specification<DataObjectItemEntity> build() {
        return SpecificationBuilder
                .create(DataObjectItemEntity.class)
                .withEquals("schemaKey", schemaKey)
                .withContains("id", id)
                .build();
    }

    public String getSchemaKey() {
        return schemaKey;
    }

    public DataObjectItemFilter setSchemaKey(String schemaKey) {
        this.schemaKey = schemaKey;
        return this;
    }

    public String getId() {
        return id;
    }

    public DataObjectItemFilter setId(String id) {
        this.id = id;
        return this;
    }
}

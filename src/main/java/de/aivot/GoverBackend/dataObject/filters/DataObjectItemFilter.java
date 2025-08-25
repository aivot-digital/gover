package de.aivot.GoverBackend.dataObject.filters;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.List;

public class DataObjectItemFilter implements Filter<DataObjectItemEntity>, Serializable {
    @Nullable
    private String schemaKey;
    @Nullable
    private String id;
    @Nullable
    private List<DataObjectFilterField> dataFields;

    public static DataObjectItemFilter create() {
        return new DataObjectItemFilter();
    }

    @Nonnull
    @Override
    public Specification<DataObjectItemEntity> build() {
        var filter = SpecificationBuilder
                .create(DataObjectItemEntity.class)
                .withEquals("schemaKey", schemaKey)
                .withContains("id", id);

        if (dataFields != null) {
            for (var field : dataFields) {
                switch (field.operator) {
                    case "eq" -> filter.withJsonEquals("data", List.of(field.path), field.value);
                    case "ne" -> filter.withJsonNotEquals("data", List.of(field.path), field.value);
                }
            }
        }

        return filter.build();
    }

    @Nullable
    public String getSchemaKey() {
        return schemaKey;
    }

    public DataObjectItemFilter setSchemaKey(@Nullable String schemaKey) {
        this.schemaKey = schemaKey;
        return this;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public DataObjectItemFilter setId(@Nullable String id) {
        this.id = id;
        return this;
    }

    @Nullable
    public List<DataObjectFilterField> getDataFields() {
        return dataFields;
    }

    public DataObjectItemFilter setDataFields(@Nullable List<DataObjectFilterField> dataFields) {
        this.dataFields = dataFields;
        return this;
    }

    public static class DataObjectFilterField implements Serializable {
        private String operator;
        private String path;
        private String value;

        public String getOperator() {
            return operator;
        }

        public DataObjectFilterField setOperator(String operator) {
            this.operator = operator;
            return this;
        }

        public String getPath() {
            return path;
        }

        public DataObjectFilterField setPath(String path) {
            this.path = path;
            return this;
        }

        public String getValue() {
            return value;
        }

        public DataObjectFilterField setValue(String value) {
            this.value = value;
            return this;
        }
    }
}

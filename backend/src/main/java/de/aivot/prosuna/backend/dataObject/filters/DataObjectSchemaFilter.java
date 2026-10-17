package de.aivot.prosuna.backend.dataObject.filters;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class DataObjectSchemaFilter implements Filter<DataObjectSchemaEntity> {
    private String name;

    @NotNull
    @Override
    public Specification<DataObjectSchemaEntity> build() {
        return SpecificationBuilder
                .create(DataObjectSchemaEntity.class)
                .withContains("name", name)
                .build();
    }

    public String getName() {
        return name;
    }

    public DataObjectSchemaFilter setName(String name) {
        this.name = name;
        return this;
    }
}

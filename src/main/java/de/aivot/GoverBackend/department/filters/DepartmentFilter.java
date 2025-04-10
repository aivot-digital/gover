package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class DepartmentFilter implements Filter<DepartmentEntity> {
    private String name;
    private Integer id;

    public static DepartmentFilter create() {
        return new DepartmentFilter();
    }

    @Override
    public Specification<DepartmentEntity> build() {
        return SpecificationBuilder
                .create(DepartmentEntity.class)
                .withContains("name", name)
                .withEquals("id", id)
                .build();
    }

    public String getName() {
        return name;
    }

    public DepartmentFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public DepartmentFilter setId(Integer id) {
        this.id = id;
        return this;
    }
}

package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessDefinitionFilter implements Filter<ProcessEntity> {
    private Integer id;
    private String name;
    private Integer departmentId;
    private Integer departmentIdNot;

    public static ProcessDefinitionFilter create() {
        return new ProcessDefinitionFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessEntity.class)
                .withEquals("id", id)
                .withContains("name", name)
                .withEquals("departmentId", departmentId)
                .withNotEquals("departmentId", departmentIdNot);

        return builder.build();
    }

    public Integer getId() {
        return id;
    }

    public ProcessDefinitionFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProcessDefinitionFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public ProcessDefinitionFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public Integer getDepartmentIdNot() {
        return departmentIdNot;
    }

    public ProcessDefinitionFilter setDepartmentIdNot(Integer departmentIdNot) {
        this.departmentIdNot = departmentIdNot;
        return this;
    }
}


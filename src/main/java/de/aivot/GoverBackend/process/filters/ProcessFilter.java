package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

import java.util.UUID;

public class ProcessFilter implements Filter<ProcessEntity> {
    private String internalTitle;
    private Integer departmentId;
    private Integer departmentIdNot;
    private UUID accessKey;

    public static ProcessFilter create() {
        return new ProcessFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessEntity.class)
                .withContains("internalTitle", internalTitle)
                .withEquals("departmentId", departmentId)
                .withNotEquals("departmentId", departmentIdNot)
                .withEquals("accessKey", accessKey);

        return builder.build();
    }

    public String getInternalTitle() {
        return internalTitle;
    }

    public ProcessFilter setInternalTitle(String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public ProcessFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public Integer getDepartmentIdNot() {
        return departmentIdNot;
    }

    public ProcessFilter setDepartmentIdNot(Integer departmentIdNot) {
        this.departmentIdNot = departmentIdNot;
        return this;
    }

    public ProcessFilter setAccessKey(UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }
}


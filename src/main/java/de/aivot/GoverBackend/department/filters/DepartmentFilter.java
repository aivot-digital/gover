package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.ShadowedOrganizationalUnitEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class DepartmentFilter implements Filter<DepartmentEntity> {
    private String name;
    private Integer id;
    private Integer themeId;

    public static DepartmentFilter create() {
        return new DepartmentFilter();
    }

    @Override
    public Specification<DepartmentEntity> build() {
        return SpecificationBuilder
                .create(DepartmentEntity.class)
                .withContains("name", name)
                .withEquals("id", id)
                .withEquals("themeId", themeId)
                .build();
    }

    public Specification<ShadowedOrganizationalUnitEntity> buildForShadowed() {
        return SpecificationBuilder
                .create(ShadowedOrganizationalUnitEntity.class)
                .withContains("name", name)
                .withEquals("id", id)
                .withEquals("themeId", themeId)
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

    public Integer getThemeId() {
        return themeId;
    }

    public DepartmentFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }
}

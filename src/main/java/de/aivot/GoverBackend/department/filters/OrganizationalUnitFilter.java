package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class OrganizationalUnitFilter implements Filter<OrganizationalUnitEntity> {
    private Integer id;
    private String name;
    private Integer themeId;

    public static OrganizationalUnitFilter create() {
        return new OrganizationalUnitFilter();
    }

    @Override
    public Specification<OrganizationalUnitEntity> build() {
        return SpecificationBuilder
                .create(OrganizationalUnitEntity.class)
                .withEquals("id", id)
                .withContains("name", name)
                .withEquals("themeId", themeId)
                .build();
    }

    public String getName() {
        return name;
    }

    public OrganizationalUnitFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public OrganizationalUnitFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public OrganizationalUnitFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }
}

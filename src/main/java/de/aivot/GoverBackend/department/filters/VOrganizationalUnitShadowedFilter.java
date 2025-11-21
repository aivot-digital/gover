package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitShadowedEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class VOrganizationalUnitShadowedFilter implements Filter<VOrganizationalUnitShadowedEntity> {
    private String name;
    private Integer id;
    private Integer themeId;

    public static VOrganizationalUnitShadowedFilter create() {
        return new VOrganizationalUnitShadowedFilter();
    }

    @Override
    public Specification<VOrganizationalUnitShadowedEntity> build() {
        return SpecificationBuilder
                .create(VOrganizationalUnitShadowedEntity.class)
                .withContains("name", name)
                .withEquals("id", id)
                .withEquals("themeId", themeId)
                .build();
    }

    public String getName() {
        return name;
    }

    public VOrganizationalUnitShadowedFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public VOrganizationalUnitShadowedFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VOrganizationalUnitShadowedFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }
}

package de.aivot.prosuna.backend.department.filters;

import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class VDepartmentShadowedFilter implements Filter<VDepartmentShadowedEntity> {
    private Integer id;
    private List<Integer> ids;
    private String name;
    private Integer themeId;
    private Boolean includeAncestors;

    public static VDepartmentShadowedFilter create() {
        return new VDepartmentShadowedFilter();
    }

    @Override
    public Specification<VDepartmentShadowedEntity> build() {
        return SpecificationBuilder
                .create(VDepartmentShadowedEntity.class)
                .withEquals("id", id)
                .withInList("id", ids)
                .withContains("name", name)
                .withEquals("themeId", themeId)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public VDepartmentShadowedFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public VDepartmentShadowedFilter setIds(List<Integer> ids) {
        this.ids = ids;
        return this;
    }

    public String getName() {
        return name;
    }

    public VDepartmentShadowedFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getThemeId() {
        return themeId;
    }

    public VDepartmentShadowedFilter setThemeId(Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public Boolean getIncludeAncestors() {
        return includeAncestors;
    }

    public VDepartmentShadowedFilter setIncludeAncestors(Boolean includeAncestors) {
        this.includeAncestors = includeAncestors;
        return this;
    }
}

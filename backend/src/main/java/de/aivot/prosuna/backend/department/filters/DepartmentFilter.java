package de.aivot.prosuna.backend.department.filters;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class DepartmentFilter implements Filter<DepartmentEntity> {
    private Integer id;
    private List<Integer> ids;
    private String name;
    private Integer themeId;

    public static DepartmentFilter create() {
        return new DepartmentFilter();
    }

    @Override
    public Specification<DepartmentEntity> build() {
        return SpecificationBuilder
                .create(DepartmentEntity.class)
                .withEquals("id", id)
                .withInList("id", ids)
                .withContains("name", name)
                .withEquals("themeId", themeId)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public DepartmentFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public DepartmentFilter setIds(List<Integer> ids) {
        this.ids = ids;
        return this;
    }

    public String getName() {
        return name;
    }

    public DepartmentFilter setName(String name) {
        this.name = name;
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

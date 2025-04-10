package de.aivot.GoverBackend.theme.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.theme.entities.Theme;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class ThemeFilter implements Filter<Theme> {
    private String name;

    public String getName() {
        return name;
    }

    public ThemeFilter setName(String name) {
        this.name = name;
        return this;
    }

    @NotNull
    @Override
    public Specification<Theme> build() {
        return SpecificationBuilder
                .create(Theme.class)
                .withContains("name", name)
                .build();
    }
}

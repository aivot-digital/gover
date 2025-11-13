package de.aivot.GoverBackend.theme.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ThemeFilter implements Filter<ThemeEntity> {
    private String name;
    private UUID logoKey;
    private UUID faviconKey;

    public static ThemeFilter create() {
        return new ThemeFilter();
    }

    @Nonnull
    @Override
    public Specification<ThemeEntity> build() {
        return SpecificationBuilder
                .create(ThemeEntity.class)
                .withContains("name", name)
                .withEquals("logoKey", logoKey)
                .withEquals("faviconKey", faviconKey)
                .build();
    }

    public String getName() {
        return name;
    }

    public ThemeFilter setName(String name) {
        this.name = name;
        return this;
    }

    public UUID getLogoKey() {
        return logoKey;
    }

    public ThemeFilter setLogoKey(UUID logoKey) {
        this.logoKey = logoKey;
        return this;
    }

    public UUID getFaviconKey() {
        return faviconKey;
    }

    public ThemeFilter setFaviconKey(UUID faviconKey) {
        this.faviconKey = faviconKey;
        return this;
    }
}

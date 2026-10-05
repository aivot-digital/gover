package de.aivot.prosuna.backend.preset.filters;

import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.preset.entities.PresetEntity;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class PresetFilter implements Filter<PresetEntity> {
    private String title;
    private Boolean published;

    public static PresetFilter create() {
        return new PresetFilter();
    }

    @Nonnull
    @Override
    public Specification<PresetEntity> build() {
        var spec = SpecificationBuilder
                .create(PresetEntity.class)
                .withContains("title", title);

        if (published != null) {
            spec = spec
                    .withNotNull("publishedVersion");
        }

        return spec
                .build();
    }

    public String getTitle() {
        return title;
    }

    public PresetFilter setTitle(String title) {
        this.title = title;
        return this;
    }
}

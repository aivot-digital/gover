package de.aivot.GoverBackend.preset.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.preset.entities.Preset;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class PresetFilter implements Filter<Preset> {
    private String title;
    private String exactTitle;
    private Boolean publishedInternally;
    private Boolean publishedToStore;

    public static PresetFilter create() {
        return new PresetFilter();
    }

    @Nonnull
    @Override
    public Specification<Preset> build() {
        var spec = SpecificationBuilder
                .create(Preset.class)
                .withContains("title", title)
                .withEquals("title", exactTitle);

        if (Boolean.TRUE.equals(publishedInternally)) {
            spec = spec.withNotNull("currentPublishedVersion");
        }

        if (Boolean.TRUE.equals(publishedToStore)) {
            spec = spec.withNotNull("currentStoreVersion");
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

    public Boolean getPublishedInternally() {
        return publishedInternally;
    }

    public PresetFilter setPublishedInternally(Boolean publishedInternally) {
        this.publishedInternally = publishedInternally;
        return this;
    }

    public Boolean getPublishedToStore() {
        return publishedToStore;
    }

    public PresetFilter setPublishedToStore(Boolean publishedToStore) {
        this.publishedToStore = publishedToStore;
        return this;
    }

    public String getExactTitle() {
        return exactTitle;
    }

    public PresetFilter setExactTitle(String exactTitle) {
        this.exactTitle = exactTitle;
        return this;
    }
}

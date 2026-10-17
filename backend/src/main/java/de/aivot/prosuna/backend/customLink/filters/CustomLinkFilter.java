package de.aivot.prosuna.backend.customLink.filters;

import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class CustomLinkFilter implements Filter<CustomLink> {
    @NotNull
    private CustomLinkType type;
    private String label;
    private Boolean enabled;

    @Nonnull
    @Override
    public Specification<CustomLink> build() {
        return SpecificationBuilder
                .create(CustomLink.class)
                .withEquals("type", type)
                .withContains("label", label)
                .withEquals("enabled", enabled)
                .build();
    }

    public CustomLinkType getType() {
        return type;
    }

    public CustomLinkFilter setType(CustomLinkType type) {
        this.type = type;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public CustomLinkFilter setLabel(String label) {
        this.label = label;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public CustomLinkFilter setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}

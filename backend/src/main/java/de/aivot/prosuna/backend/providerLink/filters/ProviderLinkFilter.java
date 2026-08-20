package de.aivot.prosuna.backend.providerLink.filters;

import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.providerLink.entities.ProviderLink;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProviderLinkFilter implements Filter<ProviderLink> {
    private String text;

    public static ProviderLinkFilter create() {
        return new ProviderLinkFilter();
    }

    @Nonnull
    @Override
    public Specification<ProviderLink> build() {
        return SpecificationBuilder
                .create(ProviderLink.class)
                .withContains("text", text)
                .build();
    }

    public String getText() {
        return text;
    }

    public ProviderLinkFilter setText(String text) {
        this.text = text;
        return this;
    }
}

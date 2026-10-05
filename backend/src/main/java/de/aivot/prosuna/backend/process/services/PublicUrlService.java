package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PublicUrlService {
    private final ProsunaConfig prosunaConfig;

    @Autowired
    public PublicUrlService(ProsunaConfig prosunaConfig) {
        this.prosunaConfig = prosunaConfig;
    }

    @Nonnull
    public String createCustomerPageUrl(@Nonnull String elementType,
                                        @Nonnull String processSlug,
                                        @Nullable String elementSlug) {
        return prosunaConfig.createUrlWithTrailingSlash(
                "/" + elementType,
                processSlug,
                elementSlug
        );
    }

    @Nonnull
    public String createPublicApiUrl(@Nonnull String elementType,
                                     @Nonnull String processSlug,
                                     @Nullable String elementSlug,
                                     @Nullable Object... additionalPathSegments) {
        if (additionalPathSegments == null) {
            additionalPathSegments = new Object[0];
        }

        var segments = new Object[3 + additionalPathSegments.length];
        segments[0] = elementType;
        segments[1] = processSlug;
        segments[2] = elementSlug;
        System.arraycopy(additionalPathSegments, 0, segments, 3, additionalPathSegments.length);

        return prosunaConfig.createUrlWithTrailingSlash(
                "/api/public",
                segments
        );
    }

    @Nonnull
    public String createPublicFormUrl(@Nonnull ProcessEntity process,
                                      @Nullable String formSlug) {
        return createCustomerPageUrl("form", process.getSlug(), formSlug);
    }

    @Nonnull
    public String createWebhookUrl(@Nonnull ProcessEntity process,
                                   @Nullable String webhookSlug,
                                   @Nullable Object... additionalPathSegments) {
        return createPublicApiUrl("webhook", process.getSlug(), webhookSlug, additionalPathSegments);
    }

    @Nonnull
    public String createProcessNamespaceDisplayPrefix() {
        // Display-only placeholder for node configuration fields; public routing still uses full generated URLs.
        return ".../prozess-namespace/";
    }
}

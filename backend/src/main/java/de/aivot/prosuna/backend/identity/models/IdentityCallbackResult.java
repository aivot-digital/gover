package de.aivot.prosuna.backend.identity.models;

import jakarta.annotation.Nonnull;

/**
 * The result of a completed identity provider callback.
 *
 * @param redirectUrl       the URL of the page that started the authentication
 * @param identitySessionId the identity session that now contains the authenticated identity
 */
public record IdentityCallbackResult(
        @Nonnull String redirectUrl,
        @Nonnull String identitySessionId
) {
}

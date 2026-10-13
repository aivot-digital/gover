package de.aivot.prosuna.backend.identity.models;

import jakarta.annotation.Nonnull;

import java.net.URI;

/**
 * The redirect to an identity provider for a started authentication.
 *
 * @param redirectUri       the authorization URI of the identity provider
 * @param flowBindingSecret the secret that binds the authentication to the browser that started it. Callers must store
 *                          it in this browser as the flow binding cookie.
 * @param callbackPath      the path of the callback, to which the flow binding cookie is restricted
 */
public record IdentityAuthenticationRedirect(
        @Nonnull URI redirectUri,
        @Nonnull String flowBindingSecret,
        @Nonnull String callbackPath
) {
}

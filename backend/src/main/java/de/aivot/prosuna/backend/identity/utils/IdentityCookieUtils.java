package de.aivot.prosuna.backend.identity.utils;

import de.aivot.prosuna.backend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.prosuna.backend.identity.models.IdentityAuthenticationRedirect;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;

public final class IdentityCookieUtils {
    public static final String IDENTITY_COOKIE_NAME = "identity_session";
    public static final String IDENTITY_COOKIE_PATH = "/api/";
    public static final String IDENTITY_FLOW_COOKIE_NAME = "identity_flow";

    private IdentityCookieUtils() {
    }

    @Nonnull
    public static Cookie createIdentityCookie(@Nonnull String identitySessionId) {
        var cookie = new Cookie(IDENTITY_COOKIE_NAME, identitySessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath(IDENTITY_COOKIE_PATH);
        return cookie;
    }

    @Nonnull
    public static Cookie createExpiredIdentityCookie() {
        var cookie = createIdentityCookie("");
        cookie.setMaxAge(0);
        return cookie;
    }

    /**
     * Creates the cookie that binds a started authentication to the current browser.
     * <p>
     * The cookie is restricted to the callback of this authentication, so concurrent authentications do not replace
     * each other. It uses {@code SameSite=Lax}, because the identity provider redirects to the callback from another
     * site.
     */
    @Nonnull
    public static Cookie createFlowBindingCookie(@Nonnull IdentityAuthenticationRedirect redirect) {
        var cookie = new Cookie(IDENTITY_FLOW_COOKIE_NAME, redirect.flowBindingSecret());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath(redirect.callbackPath());
        cookie.setMaxAge(IdentityCacheEntity.TIME_TO_LIVE_SECONDS);
        return cookie;
    }

    @Nonnull
    public static Cookie createExpiredFlowBindingCookie(@Nonnull String callbackPath) {
        var cookie = new Cookie(IDENTITY_FLOW_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath(callbackPath);
        cookie.setMaxAge(0);
        return cookie;
    }
}

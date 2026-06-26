package de.aivot.GoverBackend.identity.utils;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;

public final class IdentityCookieUtils {
    public static final String IDENTITY_COOKIE_NAME = "identity_session";
    public static final String IDENTITY_COOKIE_PATH = "/api/";

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
}

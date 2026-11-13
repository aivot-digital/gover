package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.process.cache.entities.ProcessSubmissionCacheEntity;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;

public final class ProcessSubmissionCookieUtils {
    public static final String PROCESS_SUBMISSION_COOKIE_NAME = "process_submission";

    private ProcessSubmissionCookieUtils() {
    }

    /**
     * Creates the cookie that identifies the browser that submitted the process instance.
     * <p>
     * The cookie is restricted to the public endpoints of this instance, so several submissions in one browser keep
     * their own cookie and the secret is not sent with other requests.
     */
    @Nonnull
    public static Cookie createSubmissionCookie(@Nonnull String instanceAccessKey,
                                                @Nonnull String secret) {
        var cookie = new Cookie(PROCESS_SUBMISSION_COOKIE_NAME, secret);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/api/public/processes/" + instanceAccessKey + "/");
        cookie.setMaxAge(ProcessSubmissionCacheEntity.TIME_TO_LIVE_SECONDS);
        return cookie;
    }
}

import {Location} from 'react-router-dom';

const APP_URI_QUERY_PARAM = 'app_uri';
const APP_STATE_QUERY_PARAM = 'app_state';
const AUTH_EXPIRATION_LOCAL_STORAGE_KEY = 'auth_expiration';
const CSRF_HEADER_NAME = 'X-CSRF-TOKEN';

interface JWT {
    accessExpires: number;
    refreshExpires: number;
    csrfToken?: string;
}

const STORAGE_KEY_POST_LOGIN_REDIRECT = 'oidc-post-login-redirect';

class _AuthService {
    private activeRefresh: Promise<void> | null = null;

    /**
     * Get the login URL for redirecting the user to the OIDC provider.
     */
    public getLoginUrl(location: Location): string {
        // Keep the post-login target per tab so multiple expired tabs do not overwrite each other.
        sessionStorage.setItem(STORAGE_KEY_POST_LOGIN_REDIRECT, getNormalizedCurrentRelativeUrl());

        const query = new URLSearchParams({
            [APP_URI_QUERY_PARAM]:  '/staff' + location.pathname,
            [APP_STATE_QUERY_PARAM]: location.search,
        });
        return `/api/auth/login?${query.toString()}`;
    }

    /**
     * Refresh the stored JWT if it is close to expiration.
     * If the refresh fails, the stored JWT is cleared.
     */
    public async refresh(): Promise<void> {
        if (this.activeRefresh != null) {
            await this.activeRefresh;
            return;
        }

        this.activeRefresh = (async () => {
            const response = await fetch('/api/auth/refresh', {
                credentials: 'same-origin',
            });

            if (!response.ok) {
                this.logout();
                throw new Error(`Authentication refresh failed with status ${response.status}`);
            }

            const csrfToken = response.headers.get(CSRF_HEADER_NAME);
            if (csrfToken == null || csrfToken.length === 0) {
                this.logout();
                throw new Error('Authentication refresh did not return a CSRF token');
            }

            const jwt = await response.json() as JWT;
            localStorage.setItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY, JSON.stringify({
                ...jwt,
                csrfToken,
            }));
        })();

        try {
            await this.activeRefresh;
        } finally {
            this.activeRefresh = null;
        }
    }

    /**
     * Log out the user by clearing the stored JWT.
     */
    public logout(): void {
        localStorage.removeItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY);
    }

    public getCsrfToken(): string | null {
        const storedJwt = localStorage.getItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY);
        if (storedJwt == null) {
            return null;
        }

        const jwt = JSON.parse(storedJwt) as JWT;
        return jwt.csrfToken ?? null;
    }

    /**
     * Get the expiration timestamp of the current refresh token.
     * This is in Milliseconds since the Unix epoch.
     * Returns null if no valid refresh token is available.
     */
    public getAccessExpirationTimestamp(): number | null {
        const storedJwt = localStorage.getItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY);
        if (storedJwt == null) {
            return null;
        }

        const jwt = JSON.parse(storedJwt) as JWT;

        if (this.isTokenExpired(jwt.accessExpires)) {
            return null;
        }

        return jwt.accessExpires;
    }

    /**
     * Get the expiration timestamp of the current refresh token.
     * This is in Milliseconds since the Unix epoch.
     * Returns null if no valid refresh token is available.
     */
    public getExpirationTimestamp(): number | null {
        const storedJwt = localStorage.getItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY);
        if (storedJwt == null) {
            return null;
        }

        const jwt = JSON.parse(storedJwt) as JWT;

        if (this.isTokenExpired(jwt.refreshExpires)) {
            return null;
        }

        return jwt.refreshExpires;
    }

    /**
     * Check if the user is currently authenticated.
     * A user is considered authenticated if there is a valid refresh token.
     */
    public isAuthenticated(): boolean {
        const exp = this.getExpirationTimestamp();
        return exp != null && exp > Date.now();
    }

    /**
     * Check if the user is currently authenticated.
     * A user is considered authenticated if there is a valid refresh token.
     */
    public isAccessTokenValid(): boolean {
        const exp = this.getAccessExpirationTimestamp();
        return exp != null && exp > Date.now();
    }

    /**
     * Check if a token is expired.
     *
     * @param token The token to check.
     * @private
     */
    private isTokenExpired(token: number): boolean {
        return token <= Date.now();
    }

    public consumePostLoginRedirect(): string | null {
        const redirectTarget = sessionStorage.getItem(STORAGE_KEY_POST_LOGIN_REDIRECT);
        sessionStorage.removeItem(STORAGE_KEY_POST_LOGIN_REDIRECT);

        // Only accept in-app relative targets to avoid redirecting to arbitrary locations.
        if (redirectTarget == null || !redirectTarget.startsWith('/')) {
            return null;
        }

        return redirectTarget;
    }
}

export const AuthService = new _AuthService();



function getNormalizedCurrentRelativeUrl(): string {
    const url = new URL(window.location.href);

    // Remove temporary auth/logout query params before restoring the route after login.
    [
        'code',
        'iss',
        'session_state',
        'logout',
    ].forEach((param) => {
        url.searchParams.delete(param);
    });

    const query = url.searchParams.toString();
    return `${url.pathname}${query.length > 0 ? `?${query}` : ''}${url.hash}`;
}
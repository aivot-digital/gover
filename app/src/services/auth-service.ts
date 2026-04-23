import {Location} from 'react-router-dom';

const APP_URI_QUERY_PARAM = 'app_uri';
const AUTH_EXPIRATION_LOCAL_STORAGE_KEY = 'auth_expiration';

interface JWT {
    accessExpires: number;
    refreshExpires: number;
}

class _AuthService {
    private activeRefresh: Promise<void> | null = null;

    /**
     * Get the login URL for redirecting the user to the OIDC provider.
     */
    public getLoginUrl(location: Location): string {
        const query = new URLSearchParams({
            [APP_URI_QUERY_PARAM]: '/staff' + location.pathname,
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

        this.activeRefresh = fetch('/api/auth/refresh')
            .then(r => r.json())
            .then((jwt: JWT) => {
                localStorage.setItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY, JSON.stringify(jwt));
            });

        await this.activeRefresh;

        this.activeRefresh = null;
    }

    /**
     * Log out the user by clearing the stored JWT.
     */
    public logout(): void {
        localStorage.removeItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY);
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
}

export const AuthService = new _AuthService();
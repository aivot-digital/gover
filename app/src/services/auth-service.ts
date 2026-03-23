import {getUrlWithoutQuery} from '../utils/location-utils';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../utils/string-utils';
import {dispatchApiUnreachableEvent, handleFetchError} from './base-api-service';
import {ApiError} from '../models/api-error';

const TOKEN_URL = `${AppConfig.oidc.hostname}/realms/${AppConfig.oidc.realm}/protocol/openid-connect/token`;
const AUTH_URL = `${AppConfig.oidc.hostname}/realms/${AppConfig.oidc.realm}/protocol/openid-connect/auth`;
const STORAGE_KEY_JWT = 'api-jwt';
const EXPIRATION_PADDING_SECONDS = 30; // 30 milliseconds

interface JWT_TOKEN {
    token: string;
    expires: number; // Unix timestamp in seconds
}

interface JWT {
    access: JWT_TOKEN;
    refresh: JWT_TOKEN;
}

interface OidcJWT {
    access_token: string;
    expires_in: number; // in seconds
    refresh_token: string;
    refresh_expires_in: number; // in seconds
}

const OidcCodeVerifierLength = 48;
const OidcCodeVerifierLocalStorageKey = 'oidc_code_verifier';

const DEFAULT_TIMEOUT = 5000; // 5 seconds

const DefaultUnauthorizedApiError: ApiError = {
    status: 401,
    message: 'Sie sind nicht angemeldet',
    details: null,
    displayableToUser: true,
};

export class AuthService {
    /**
     * Get the login URL for redirecting the user to the OIDC provider.
     */
    public async getLoginUrl(): Promise<string> {
        let oidcCodeVerifier = localStorage.getItem(OidcCodeVerifierLocalStorageKey);
        if (oidcCodeVerifier == null || isStringNullOrEmpty(oidcCodeVerifier)) {
            oidcCodeVerifier = createRandomString(OidcCodeVerifierLength);
            localStorage.setItem(OidcCodeVerifierLocalStorageKey, oidcCodeVerifier);
        }

        const oidcCodeChallenge = await getSHA256BinaryValue(oidcCodeVerifier);

        const query = new URLSearchParams({
            client_id: AppConfig.oidc.client,
            code_challenge_method: 'S256',
            code_challenge: oidcCodeChallenge,
            response_type: 'code',
            scope: 'openid profile email',
            redirect_uri: getUrlWithoutQuery(),
        });

        if (isStringNotNullOrEmpty(AppConfig.oidc.idp_hint)) {
            query.append('kc_idp_hint', AppConfig.oidc.idp_hint);
        }

        return `${AUTH_URL}?${query.toString()}`;
    }

    /**
     * Authenticate the user using the authorization code and store the JWT in local storage.
     *
     * @param authorizationCode The authorization code received from the OIDC provider.
     * @param signal Optional AbortSignal to cancel the request.
     */
    public async authenticate(authorizationCode: string, signal?: AbortSignal): Promise<void> {
        const oidc = await this.fetchJWT(authorizationCode, signal);
        if (oidc == null) {
            throw new Error('Failed to fetch JWT');
        }

        const jwt = this.buildJwtFromOidc(oidc);

        this.setLocalStorageJWT(jwt);
    }

    /**
     * Log out the user by clearing the stored JWT.
     */
    public logout(): void {
        this.setLocalStorageJWT(null);
    }

    /**
     * Get a valid access token, refreshing it if necessary.
     * Returns null if no valid token is available.
     *
     * @param signal Optional AbortSignal to cancel the request.
     * @param throwUnauthorizedException If true, throws an ApiError if the user is unauthorized.
     */
    public async getAccessToken(signal: AbortSignal | undefined | null, throwUnauthorizedException: boolean = true): Promise<string | null> {
        // Get the stored JWT from local storage
        const storedJwt = this.getLocalStorageJWT();
        if (storedJwt == null) {
            if (throwUnauthorizedException) {
                throw DefaultUnauthorizedApiError;
            }
            return null;
        }

        // If the access token is still valid, return it
        if (!this.isTokenExpired(storedJwt.access)) {
            return storedJwt.access.token;
        }

        // If the refresh token is expired, return null
        if (this.isTokenExpired(storedJwt.refresh)) {
            if (throwUnauthorizedException) {
                throw DefaultUnauthorizedApiError;
            }
            return null;
        }

        // Refresh the JWT using the refresh token
        const refreshedOidcJwt = await this.refreshJWT(storedJwt, signal);

        // If refreshing failed, clear the stored JWT and return null
        if (refreshedOidcJwt == null) {
            this.setLocalStorageJWT(null);
            if (throwUnauthorizedException) {
                throw DefaultUnauthorizedApiError;
            }
            return null;
        }

        // Build a new JWT from the refreshed OIDC JWT
        const newJwt = this.buildJwtFromOidc(refreshedOidcJwt);

        // Store the new JWT in local storage
        this.setLocalStorageJWT(newJwt);

        // Return the new access token
        return newJwt.access.token;
    }

    /**
     * Get the expiration timestamp of the current refresh token.
     * This is in Milliseconds since the Unix epoch.
     * Returns null if no valid refresh token is available.
     */
    public getExpirationTimestamp(): number | null {
        const storedJwt = this.getLocalStorageJWT();
        if (storedJwt == null) {
            return null;
        }

        if (this.isTokenExpired(storedJwt.refresh)) {
            return null;
        }

        return storedJwt.refresh.expires;
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
     * Refresh the stored JWT if it is close to expiration.
     * If the refresh fails, the stored JWT is cleared.
     *
     * @param signal Optional AbortSignal to cancel the request.
     */
    public async refresh(signal?: AbortSignal): Promise<void> {
        const storedJwt = this.getLocalStorageJWT();
        if (storedJwt == null) {
            return;
        }

        const oidcJwt = await this.refreshJWT(storedJwt, signal);
        if (oidcJwt == null) {
            this.setLocalStorageJWT(null);
            return;
        }

        const newJwt = this.buildJwtFromOidc(oidcJwt);

        this.setLocalStorageJWT(newJwt);
    }

    /**
     * Fetch a new JWT using the authorization code.
     *
     * @param authorizationCode The authorization code received from the OIDC provider.
     * @param signal Optional AbortSignal to cancel the request.
     */
    private async fetchJWT(authorizationCode: string, signal?: AbortSignal): Promise<OidcJWT | null> {
        const oidcCodeVerifier = localStorage.getItem(OidcCodeVerifierLocalStorageKey) ?? '';

        const payload = new URLSearchParams({
            code_verifier: oidcCodeVerifier,
            grant_type: 'authorization_code',
            client_id: AppConfig.oidc.client,
            code: authorizationCode,
            redirect_uri: getUrlWithoutQuery(),
        });

        let response: Response;
        try {
            response = await fetch(TOKEN_URL, {
                method: 'POST',
                body: payload,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                },
                signal: signal ?? AbortSignal.timeout(DEFAULT_TIMEOUT),
            });
        } catch(error: any) {
            response = handleFetchError(error);
        }

        if (response.status !== 200) {
            if (response.status > 500) {
                dispatchApiUnreachableEvent();
            }

            return null;
        }

        localStorage.removeItem(OidcCodeVerifierLocalStorageKey);

        return await response.json() as OidcJWT;
    }

    /**
     * Refresh the JWT using the refresh token.
     *
     * @param jwt The current JWT containing the refresh token.
     * @param signal Optional AbortSignal to cancel the request.
     */
    private async refreshJWT(jwt: JWT, signal: AbortSignal | undefined | null): Promise<OidcJWT | null> {
        const payload = new URLSearchParams({
            grant_type: 'refresh_token',
            client_id: AppConfig.oidc.client,
            refresh_token: jwt.refresh.token,
        });

        let response: Response;
        try {
            response = await fetch(TOKEN_URL, {
                method: 'POST',
                body: payload,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                },
                signal: signal ?? AbortSignal.timeout(DEFAULT_TIMEOUT),
            });
        } catch(error: any) {
            response = handleFetchError(error);
        }

        if (response.status !== 200) {
            if (response.status > 500) {
                dispatchApiUnreachableEvent();
            }

            return null;
        }

        return await response.json() as OidcJWT;
    }

    /**
     * Build a JWT object from the OIDC JWT response.
     * Pads the expiration times to account for network delays.
     *
     * @param oidc The OIDC JWT response.
     * @private
     */
    private buildJwtFromOidc(oidc: OidcJWT): JWT {
        const accessTokenExpirationTimestamp = Date.now() + ((oidc.expires_in - EXPIRATION_PADDING_SECONDS) * 1000);
        const refreshTokenExpirationTimestamp = Date.now() + ((oidc.refresh_expires_in - EXPIRATION_PADDING_SECONDS) * 1000);

        return {
            access: {
                token: oidc.access_token,
                expires: accessTokenExpirationTimestamp,
            },
            refresh: {
                token: oidc.refresh_token,
                expires: refreshTokenExpirationTimestamp,
            },
        };
    }

    /**
     * Get the JWT stored in local storage.
     * Returns null if no JWT is stored or if parsing fails.
     *
     * @private
     */
    private getLocalStorageJWT(): JWT | null {
        const jwtStr = localStorage.getItem(STORAGE_KEY_JWT);
        if (jwtStr == null) {
            return null;
        }

        try {
            return JSON.parse(jwtStr) as JWT;
        } catch {
            return null;
        }
    }

    /**
     * Store the JWT in local storage.
     * If the JWT is null, it removes it from storage.
     *
     * @param jwt The JWT to store, or null to remove it.
     * @private
     */
    private setLocalStorageJWT(jwt: JWT | null) {
        if (jwt == null) {
            localStorage.removeItem(STORAGE_KEY_JWT);
        } else {
            const str = JSON.stringify(jwt);
            localStorage.setItem(STORAGE_KEY_JWT, str);
        }
    }

    /**
     * Check if a token is expired.
     *
     * @param token The token to check.
     * @private
     */
    private isTokenExpired(token: JWT_TOKEN): boolean {
        return token.expires <= Date.now();
    }
}

function createRandomString(length: number) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    const randomArray = new Uint8Array(length);
    crypto.getRandomValues(randomArray);
    randomArray.forEach((number) => {
        result += chars[number % chars.length];
    });
    return result;
}

async function getSHA256BinaryValue(input: string) {
    const textEncoder = new TextEncoder();
    const textAsBuffer = textEncoder.encode(input);

    const hashBuffer = await crypto
        .subtle
        .digest('SHA-256', textAsBuffer);

    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const binString = String.fromCodePoint(...hashArray);
    const encodedHash = btoa(binString)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '')

    return encodedHash;
}
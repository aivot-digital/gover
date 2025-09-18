export interface RequestOptions {
    abort?: AbortSignal;
    headers?: Record<string, string>;
    query?: Record<string, string | number | boolean | undefined> | URLSearchParams;
}

export class BaseApiService {
    public async get<T>(path: string, options?: RequestOptions): Promise<T> {
        const accessToken = await fetchApiToken(options?.abort);

        const response = await fetch(combineUrl(path, options), {
            method: 'GET',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`GET ${path} failed with status ${response.status}`);
        }

        return await response.json() as T;
    }

    public async getUnauthenticated<T>(path: string, options?: RequestOptions): Promise<T> {
        const response = await fetch(combineUrl(path, options), {
            method: 'GET',
            headers: combineHeaders({'Content-Type': 'application/json'}, options),
            signal: options?.abort,
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`GET ${path} failed with status ${response.status}`);
        }

        return await response.json() as T;
    }

    public async getBlob(path: string, options?: RequestOptions): Promise<Blob> {
        const accessToken = await fetchApiToken(options?.abort);

        const response = await fetch(combineUrl(path, options), {
            method: 'GET',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`GET ${path} failed with status ${response.status}`);
        }

        return await response.blob();
    }

    public async getBlobUnauthenticated(path: string, options?: RequestOptions): Promise<Blob> {
        const response = await fetch(combineUrl(path, options), {
            method: 'GET',
            headers: combineHeaders({'Content-Type': 'application/json'}, options),
            signal: options?.abort,
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`GET ${path} failed with status ${response.status}`);
        }

        return await response.blob();
    }

    public async post<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const accessToken = await fetchApiToken();

        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
            body: JSON.stringify(body),
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`POST ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async postUnauthenticated<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders({'Content-Type': 'application/json'}, options),
            signal: options?.abort,
            body: JSON.stringify(body),
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`POST ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async postFormData<R>(path: string, formData: FormData, options?: RequestOptions): Promise<R> {
        const accessToken = await fetchApiToken(options?.abort);

        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders({'Authorization': `Bearer ${accessToken}`}, options),
            signal: options?.abort,
            body: formData,
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`POST ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async postFormDataUnauthenticated<R>(path: string, formData: FormData, options?: RequestOptions): Promise<R> {
        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders({}, options),
            signal: options?.abort,
            body: formData,
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`POST ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async postFormUrlEncoded<R>(path: string, formData: URLSearchParams, options?: RequestOptions): Promise<R> {
        const accessToken = await fetchApiToken(options?.abort);

        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders({
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
            }, options),
            signal: options?.abort,
            body: formData.toString(),
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`POST ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async postFormUrlEncodedUnauthenticated<R>(path: string, formData: URLSearchParams, options?: RequestOptions): Promise<R> {
        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders({
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
            }, options),
            signal: options?.abort,
            body: formData.toString(),
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`POST ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async put<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const accessToken = await fetchApiToken();

        const response = await fetch(combineUrl(path, options), {
            method: 'PUT',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
            body: JSON.stringify(body),
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`PUT ${path} failed with status ${response.status}`);
        }

        return await response.json() as R;
    }

    public async delete(path: string, options?: RequestOptions): Promise<void> {
        const accessToken = await fetchApiToken(options?.abort);

        const response = await fetch(combineUrl(path, options), {
            method: 'DELETE',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
        });

        if (response.status !== 200 && response.status !== 204) {
            if (response.status === 401) {
                resetLocalStorageJwt();
            }
            throw new Error(`DELETE ${path} failed with status ${response.status}`);
        }
    }
}

export const STORAGE_KEY_JWT = 'api-jwt';
const EXPIRATION_PADDING_MS = 30;

interface JWT {
    access: {
        token: string;
        expires: number; // Unix timestamp in seconds
    };
    refresh: {
        token: string;
        expires: number; // Unix timestamp in seconds
    };
}

async function fetchApiToken(signal?: AbortSignal): Promise<string> {
    let jwt = getLocalStorageJwt();

    // If there is no JWT in local storage, or if the refresh token has expired, clear local storage and throw an error
    if (jwt == null) {
        resetLocalStorageJwt();
        throw new Error();
    }

    if (jwt.access.expires <= Date.now()) {
        jwt = await getApiJWT(jwt.refresh.token, signal);
    }

    if (jwt == null) {
        resetLocalStorageJwt();
        throw new Error();
    }

    return jwt.access.token;
}

export function getLocalStorageJwt(): JWT | null {
    const jwtStr = localStorage.getItem(STORAGE_KEY_JWT);
    if (jwtStr == null) {
        return null;
    }

    let jwt: JWT | null = null;
    try {
        jwt = JSON.parse(jwtStr) as JWT;
    } catch {
        return null;
    }

    if (jwt.refresh.expires <= Date.now()) {
        return null;
    }

    return jwt;
}

async function getApiJWT(refreshToken: string, signal?: AbortSignal): Promise<JWT | null> {
    const url = `${AppConfig.oidc.hostname}/realms/${AppConfig.oidc.realm}/protocol/openid-connect/token`;
    const body = new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: AppConfig.oidc.client,
        refresh_token: refreshToken,
    });
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    };

    const response = await window.fetch(url, {
        method: 'POST',
        body: body,
        headers: headers,
        signal: signal,
    });

    if (response.status !== 200) {
        return null;
    }

    const data = await response
        .json();

    const jwt: JWT = {
        access: {
            token: data.access_token,
            expires: (Date.now() + (data.expires_in * 1000)) - EXPIRATION_PADDING_MS,
        },
        refresh: {
            token: data.refresh_token,
            expires: (Date.now() + (data.refresh_expires_in * 1000)) - EXPIRATION_PADDING_MS,
        },
    };

    storeLocalStorageJwt(jwt);

    return jwt;
}

export function storeLocalStorageJwt(jwt: JWT) {
    localStorage.setItem(STORAGE_KEY_JWT, JSON.stringify(jwt));
}

export function resetLocalStorageJwt() {
    localStorage.removeItem(STORAGE_KEY_JWT);
}

function combineUrl(path: string, options?: RequestOptions): string {
    if (options?.query == null) {
        return path;
    }

    let queryStr: string;
    if (options.query instanceof URLSearchParams) {
        queryStr = options.query.toString();
    } else {
        const params = new URLSearchParams();
        for (const [key, value] of Object.entries(options.query)) {
            if (value != null) {
                params.append(key, String(value));
            }
        }
        queryStr = params.toString();
    }

    if (queryStr.length === 0) {
        return path;
    }

    return `${path}?${queryStr}`;
}

function combineHeaders(def: Record<string, any>, options?: RequestOptions): Record<string, string> {
    if (options?.headers == null) {
        return def;
    }

    return {
        ...def,
        ...options.headers,
    };
}

function createDefaultHeaders(accessToken: string): Record<string, string> {
    return {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
    };
}

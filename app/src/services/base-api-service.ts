import {AuthService} from './auth-service';
import {ApiError, createApiError} from '../models/api-error';

export interface RequestOptions {
    abort?: AbortSignal;
    headers?: Record<string, string>;
    query?: Record<string, string | number | boolean | undefined> | URLSearchParams;
}

const DefaultUnauthorizedApiError: ApiError = {
    status: 401,
    message: 'Sie sind nicht angemeldet',
    details: null,
    displayableToUser: true,
};

export class BaseApiService {
    private readonly auth = new AuthService();

    public async get<T>(path: string, options?: RequestOptions): Promise<T> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

        const response = await fetch(combineUrl(path, options), {
            method: 'GET',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                this.auth.logout();
            }
            throw await createApiError(response);
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
            throw await createApiError(response);
        }

        return await response.json() as T;
    }

    public async getBlob(path: string, options?: RequestOptions): Promise<Blob> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

        const response = await fetch(combineUrl(path, options), {
            method: 'GET',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                this.auth.logout();
            }
            throw await createApiError(response);
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
            throw await createApiError(response);
        }

        return await response.blob();
    }

    public async post<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
            body: JSON.stringify(body),
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                this.auth.logout();
            }
            throw await createApiError(response);
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
            throw await createApiError(response);
        }

        return await response.json() as R;
    }

    public async postFormData<R>(path: string, formData: FormData, options?: RequestOptions): Promise<R> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

        const response = await fetch(combineUrl(path, options), {
            method: 'POST',
            headers: combineHeaders({'Authorization': `Bearer ${accessToken}`}, options),
            signal: options?.abort,
            body: formData,
        });

        if (response.status !== 200 && response.status !== 201) {
            if (response.status === 401) {
                this.auth.logout();
            }
            throw await createApiError(response);
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
            throw await createApiError(response);
        }

        return await response.json() as R;
    }

    public async postFormUrlEncoded<R>(path: string, formData: URLSearchParams, options?: RequestOptions): Promise<R> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

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
                this.auth.logout();
            }
            throw await createApiError(response);
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
            throw await createApiError(response);
        }

        return await response.json() as R;
    }

    public async put<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

        const response = await fetch(combineUrl(path, options), {
            method: 'PUT',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
            body: JSON.stringify(body),
        });

        if (response.status !== 200) {
            if (response.status === 401) {
                this.auth.logout();
            }
            throw await createApiError(response);
        }

        return await response.json() as R;
    }

    public async delete(path: string, options?: RequestOptions): Promise<void> {
        const accessToken = await this.auth.getAccessToken(options?.abort);
        if (accessToken == null) {
            throw DefaultUnauthorizedApiError;
        }

        const response = await fetch(combineUrl(path, options), {
            method: 'DELETE',
            headers: combineHeaders(createDefaultHeaders(accessToken), options),
            signal: options?.abort,
        });

        if (response.status !== 200 && response.status !== 204) {
            if (response.status === 401) {
                this.auth.logout();
            }
            throw await createApiError(response);
        }
    }
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

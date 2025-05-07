import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {ApiError} from '../models/api-error';
import {AuthData} from '../models/dtos/auth-data';
import {AppConfig} from '../app-config';

type QueryParamsValue = string | number | boolean | undefined | null;

type QueryParams = Record<string, QueryParamsValue | Array<QueryParamsValue>>;

export interface ApiOptions {
    requestOptions?: RequestInit;
    queryParams?: QueryParams;
    abortController?: AbortController;
}

interface Auth {
    authData: AuthData;
    onAuthDataChange: (authData: AuthDataDto) => void;
}

export class ApiService {
    protected readonly auth: Auth | undefined;

    public constructor(auth?: Auth) {
        this.auth = auth;
    }

    private static appendQueryParams(url: string, options?: ApiOptions): string {
        if (options == null || options.queryParams == null || Object.keys(options.queryParams).length === 0) {
            return url;
        }

        const searchParams = new URLSearchParams();

        for (const [key, value] of Object.entries(options.queryParams)) {
            if (value != null) {
                if (typeof value === 'boolean') {
                    searchParams.append(key, value ? 'true' : 'false');
                } else if (Array.isArray(value)) {
                    for (const item of value) {
                        if (item != null) {
                            searchParams.append(key, item.toString());
                        }
                    }
                } else {
                    searchParams.append(key, value.toString());
                }
            }
        }

        return `${url}?${searchParams.toString()}`;
    }

    public getAuthData(): AuthData | undefined {
        return this.auth?.authData;
    }

    public isAuthenticated() {
        return (
            this.auth != null &&
            this.auth.authData != null &&
            this.auth.authData.refreshToken != null &&
            this.auth.authData.refreshToken.expires > Date.now()
        );
    }

    protected async getAccessToken(abortController?: AbortController): Promise<string | undefined> {
        if (this.auth == null) {
            return undefined;
        }

        const authData = this.auth.authData;
        const onAuthDataChange = this.auth.onAuthDataChange;

        if (authData.accessToken != null && authData.accessToken.expires > Date.now()) {
            return authData.accessToken.token;
        }

        if (authData.refreshToken == null || authData.refreshToken.expires < Date.now()) {
            return undefined;
        }

        const response = await window.fetch(`${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/protocol/openid-connect/token`, {
            method: 'POST',
            body: new URLSearchParams({
                grant_type: 'refresh_token',
                client_id: AppConfig.staff.client,
                refresh_token: authData.refreshToken.token,
            }),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
            },
            signal: abortController?.signal,
        });

        if (response.status !== 200) {
            return undefined;
        }

        const refreshedAuthData: AuthDataDto = await response.json();
        onAuthDataChange(refreshedAuthData);
        return refreshedAuthData.access_token;
    }

    public async get<T>(url: string, headers?: any, options?: ApiOptions): Promise<T> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            {'Content-Type': 'application/json'},
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            headers,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'GET',
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });

        if (response.status !== 200) {
            throw await this.creatApiError(response);
        }

        return await response.json();
    }

    public async getBlob(url: string, options?: ApiOptions): Promise<Blob> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            {'Content-Type': 'application/octet-stream'},
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'GET',
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });
        if (response.status !== 200) {
            throw await this.creatApiError(response);
        }
        return await response.blob();
    }

    public async post<T>(url: string, data: any, options?: ApiOptions): Promise<T> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            {'Content-Type': 'application/json'},
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'POST',
            body: JSON.stringify(data),
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });
        if (response.status !== 200 && response.status !== 201) {
            throw await this.creatApiError(response);
        }
        return await response.json();
    }

    public async postFormData<T>(url: string, data: FormData, options?: ApiOptions): Promise<T> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'POST',
            body: data,
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });
        if (response.status !== 200 && response.status !== 201) {
            throw await this.creatApiError(response);
        }
        return await response.json();
    }

    public async postFormUrlEncoded<T>(url: string, data: Record<string, string>, options?: ApiOptions): Promise<T> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'POST',
            body: new URLSearchParams(data),
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });
        if (response.status !== 200 && response.status !== 201) {
            throw await this.creatApiError(response);
        }
        return await response.json();
    }

    public async put<T>(url: string, data: any, options?: ApiOptions): Promise<T> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            {'Content-Type': 'application/json'},
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'PUT',
            body: JSON.stringify(data),
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });
        if (response.status !== 200) {
            throw await this.creatApiError(response);
        }
        return await response.json();
    }

    public async delete(url: string, options?: ApiOptions): Promise<void> {
        const accessToken = await this.getAccessToken();

        const combinedHeaders = combineHeaders(
            {'Content-Type': 'application/json'},
            accessToken != null ? {Authorization: `Bearer ${accessToken}`} : undefined,
            options?.requestOptions?.headers,
        );
        delete options?.requestOptions?.headers;

        const response = await window.fetch(ApiService.appendQueryParams(url, options), {
            method: 'DELETE',
            headers: combinedHeaders,
            signal: options?.abortController?.signal,
            ...options?.requestOptions,
        });
        if (response.status !== 200 && response.status !== 204) {
            throw await this.creatApiError(response);
        }
    }

    private async creatApiError(response: Response): Promise<ApiError> {
        let details = await response.text();
        try {
            details = JSON.parse(details);
        } catch (err) {
            // Ignore parse error
        }
        return new ApiError(response.status, details);
    }
}

function combineHeaders(...header: Array<HeadersInit | Record<string, string> | undefined | null>): Record<string, string> {
    let combined = {};
    for (const h of header) {
        if (h != null) {
            combined = {
                ...combined,
                ...h,
            };
        }
    }
    return combined;
}
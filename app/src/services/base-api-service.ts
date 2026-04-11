import {AuthService} from './auth-service';
import {ApiError, createApiError} from '../models/api-error';
import {createApiPath} from '../utils/url-path-utils';
import {isStringNotNullOrEmpty} from '../utils/string-utils';

export type QueryParams = Record<string, string | number | boolean | string[] | undefined> | URLSearchParams;

export interface RequestOptions {
    abort?: AbortSignal;
    headers?: Record<string, string | null>;
    query?: QueryParams;
    doNotHandleStatusCodes?: boolean;
    skipAuthCheck?: boolean;
}

const DEFAULT_TIMEOUT = 1000 * 60; // 1 Minute
export const API_EVENT_UNREACHABLE = 'api-event-unreachble';

export class BaseApiService {
    private readonly auth = new AuthService();

    public async get<T>(path: string, options?: RequestOptions): Promise<T> {
        const response = await this.fetch('GET', path, undefined, options);
        return await response.json() as T;
    }

    public async getBlob(path: string, options?: RequestOptions): Promise<Blob> {
        const response = await this.fetch('GET', path, undefined, {
            ...options,
            headers: {
                ...options?.headers,
                'Accept': 'application/octet-stream',
            },
        });
        return await response.blob();
    }

    public async post<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const response = await this.fetch('POST', path, JSON.stringify(body), options);
        return await response.json() as R;
    }

    public async postXml<T, R>(path: string, body: ArrayBuffer | string, options?: RequestOptions): Promise<R> {
        const response = await this.fetch('POST', path, body, {
            ...options,
            headers: {
                ...options?.headers,
                'Content-Type': 'application/xml',
                'Accept': 'application/json',
            },
        });
        return await response.json() as R;
    }

    public async postFormData<R>(path: string, formData: FormData, options?: RequestOptions): Promise<R> {
        const response = await this.fetch('POST', path, formData,  {
            ...options,
            headers: {
                ...options?.headers,
                'Content-Type': null, // Let the browser set the correct Content-Type with boundary
            },
        });
        return await response.json() as R;
    }

    public async putFormData<R>(path: string, formData: FormData, options?: RequestOptions): Promise<R> {
        const response = await this.fetch('PUT', path, formData,  {
            ...options,
            headers: {
                ...options?.headers,
                'Content-Type': null, // Let the browser set the correct Content-Type with boundary
            },
        });
        return await response.json() as R;
    }

    public async postFormUrlEncoded<R>(path: string, formData: URLSearchParams, options?: RequestOptions): Promise<R> {
        const response = await this.fetch('POST', path, formData.toString(), {
            ...options,
            headers: {
                ...options?.headers,
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
            },
        });

        return await response.json() as R;
    }

    public async put<T, R>(path: string, body: T, options?: RequestOptions): Promise<R> {
        const response = await this.fetch('PUT', path, JSON.stringify(body), options);
        return await response.json() as R;
    }

    public async putWithoutResponse<T>(path: string, body: T, options?: RequestOptions): Promise<void> {
        await this.fetch('PUT', path, JSON.stringify(body), options);
    }

    public async delete(path: string, options?: RequestOptions): Promise<void> {
        await this.fetch('DELETE', path, undefined, options);
    }

    public async fetch(method: string, path: string, body?: any, options?: RequestOptions): Promise<Response> {
        const accessToken = await this
            .auth
            .getAccessToken(options?.abort, options?.skipAuthCheck !== true);

        let response: Response;
        try {
            response = await fetch(this.combineUrl(path, options), {
                method: method,
                headers: this.combineHeaders(this.createDefaultHeaders(accessToken), options),
                signal: options?.abort ?? AbortSignal.timeout(DEFAULT_TIMEOUT),
                body: body,
            });
        } catch (error: any) {
            response = handleFetchError(error);
        }

        if (response.status >= 400) {
            if (options?.doNotHandleStatusCodes !== true) {
                if (response.status === 401) {
                    this.auth.logout();
                }
                if (response.status > 500) {
                    dispatchApiUnreachableEvent();
                }
            }
            throw await createApiError(response);
        }

        return response;
    }

    protected combineUrl(path: string, options?: RequestOptions): string {
        return this.createPath(path, options?.query);
    }

    public createPath(path: string, queryParams?: QueryParams): string {
        if (queryParams == null) {
            return createApiPath(path);
        }

        let queryStr: string;
        if (queryParams instanceof URLSearchParams) {
            queryStr = queryParams.toString();
        } else {
            const params = new URLSearchParams();
            for (const [key, value] of Object.entries(queryParams)) {
                if (value != null) {
                    if (Array.isArray(value)) {
                        for (const v of value) {
                            params.append(key, v);
                        }
                    } else {
                        params.append(key, String(value));
                    }
                }
            }
            queryStr = params.toString();
        }

        if (queryStr.length === 0) {
            return createApiPath(path);
        }

        return createApiPath(`${path}?${queryStr}`);
    }

    protected combineHeaders(def: Record<string, any>, options?: RequestOptions): Record<string, string> {
        const protoHeaders = {
            ...def,
            ...options?.headers,
        };

        const headers: Record<string, string> = {};
        for (const [key, value] of Object.entries(protoHeaders)) {
            if (isStringNotNullOrEmpty(value)) {
                headers[key] = String(value);
            }
        }

        return headers;
    }

    protected createDefaultHeaders(accessToken: string | undefined | null): Record<string, string> {
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        };

        if (accessToken != null) {
            headers['Authorization'] = `Bearer ${accessToken}`;
        }

        return headers;
    }
}

export function handleFetchError(error: any): Response {
    console.log(error.message);
    console.log(error.name);
    console.log(JSON.stringify(error));
    if (error.name === 'TimeoutError') {
        const payload: ApiError = {
            status: 504,
            details: null,
            message: 'Die Anfrage hat zu lange gedauert und wurde abgebrochen. Versuchen Sie es später erneut.',
            displayableToUser: true,
        };

        return Response.json(payload, {
            status: 504,
            statusText: 'Gateway Timeout',
            headers: {},
        });
    }

    if (error.name === 'TypeError') {
        const payload: ApiError = {
            status: 503,
            details: null,
            message: 'Der Server ist nicht erreichbar. Bitte überprüfen Sie Ihre Internetverbindung und versuchen Sie es erneut.',
            displayableToUser: true,
        };

        return Response.json(payload, {
            status: 503,
            statusText: 'Service Unavailable',
            headers: {},
        });
    }

    throw error;
}

export function dispatchApiUnreachableEvent(): void {
    window.dispatchEvent(new Event(API_EVENT_UNREACHABLE));
}

import {useAppDispatch} from './use-app-dispatch';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {useAppSelector} from './use-app-selector';
import {clearAuthData, selectAuthData, setAuthData} from '../slices/auth-slice';
import {isApiError} from '../models/api-error';
import {ApiOptions, ApiService} from '../services/api-service';
import {Api} from '@mui/icons-material';
import {BaseApiService, RequestOptions} from '../services/base-api-service';
import {AuthService} from '../services/auth-service';

export interface Api {
    isAuthenticated: boolean;

    get<T>(url: string, options?: ApiOptions): Promise<T>;

    getPublic<T>(url: string, options?: ApiOptions): Promise<T>;

    getBlob(url: string, options?: ApiOptions): Promise<Blob>;

    post<T>(url: string, data: any, options?: ApiOptions): Promise<T>;

    postFormData<T>(url: string, data: FormData, options?: ApiOptions): Promise<T>;
    putFormData<T>(url: string, data: FormData, options?: ApiOptions): Promise<T>;

    postFormUrlEncoded<T>(url: string, data: Record<string, string>, options?: ApiOptions): Promise<T>;

    postXML<T>(url: string, data: string | ArrayBuffer, options?: ApiOptions): Promise<T>;

    put<T>(url: string, data: any, options?: ApiOptions): Promise<T>;

    destroy<T>(url: string, options?: ApiOptions): Promise<void>;
}

export function useApi(): Api {
    const dispatch = useAppDispatch();
    const authData = useAppSelector(selectAuthData);

    const [isAuthenticated, setIsAuthenticated] = useState(false);

    const serviceRef = useRef<ApiService>(new ApiService());

    useEffect(() => {
        if (authData != null) {
            serviceRef.current = new ApiService({
                authData: authData,
                onAuthDataChange: (authData: AuthDataDto) => {
                    dispatch(setAuthData(authData));
                },
            });
            setIsAuthenticated(true);
        } else {
            serviceRef.current = new ApiService();
            setIsAuthenticated(false);
        }
    }, [authData]);

    const handleUnauthorized = useCallback((err: any) => {
        if (isApiError(err) && err.status === 401) {
            dispatch(clearAuthData());
        }
        return err;
    }, []);

    return useMemo(() => {
        return baseApiServiceAsApi();
    }, [isAuthenticated]);
}

function baseApiServiceAsApi(): Api {
    const auth = new AuthService();
    const api = new BaseApiService();

    return {
        isAuthenticated: auth.isAuthenticated(),
        get: async <T>(url: string, options?: ApiOptions): Promise<T> => {
            return await api
                .get<T>(`/api/${url}`, apiOptionsToRequestOptions(options));
        },
        getPublic: async <T>(url: string, options?: ApiOptions): Promise<T> => {
            return await api
                .get<T>(`/api/public/${url}`, {
                    ...apiOptionsToRequestOptions(options),
                    skipAuthCheck: true,
                });
        },
        getBlob: async (url: string, options?: ApiOptions): Promise<Blob> => {
            return await api
                .getBlob(`/api/${url}`, apiOptionsToRequestOptions(options));
        },
        post: async <T>(url: string, data: any, options?: ApiOptions): Promise<T> => {
            return await api
                .post(`/api/${url}`, data, {
                    ...apiOptionsToRequestOptions(options),
                    skipAuthCheck: true,
                });
        },
        postXML: async <T>(url: string, data: ArrayBuffer | string, options?: ApiOptions): Promise<T> => {
            return await api
                .postXml(`/api/${url}`, data, apiOptionsToRequestOptions(options));
        },
        postFormData: async <T>(url: string, data: FormData, options?: ApiOptions): Promise<T> => {
            return await api
                .postFormData(`/api/${url}`, data, apiOptionsToRequestOptions(options));
        },
        putFormData: async <T>(url: string, data: FormData, options?: ApiOptions): Promise<T> => {
            return await api
                .putFormData(`/api/${url}`, data, apiOptionsToRequestOptions(options));
        },
        postFormUrlEncoded: async <T>(url: string, data: Record<string, string>, options?: ApiOptions): Promise<T> => {
            return await api
                .postFormUrlEncoded(`/api/${url}`, new URLSearchParams(data), apiOptionsToRequestOptions(options));
        },
        put: async <T>(url: string, data: any, options?: ApiOptions): Promise<T> => {
            return await api
                .put(`/api/${url}`, data, apiOptionsToRequestOptions(options));
        },
        destroy: async <T>(url: string, options?: ApiOptions): Promise<void> => {
            return await api
                .delete(`/api/${url}`, apiOptionsToRequestOptions(options));
        },
    };
}

function apiOptionsToRequestOptions(options?: ApiOptions): RequestOptions | undefined {
    if (options == null) {
        return undefined;
    }

    const requestOptions: RequestOptions = {};

    if (options.queryParams) {
        const params = new URLSearchParams();
        for (const [key, value] of Object.entries(options.queryParams)) {
            if (value !== undefined && value !== null) {
                params.append(key, String(value));
            }
        }
        requestOptions.query = params;
    }

    if (options.requestOptions != null) {
        requestOptions.headers = options.requestOptions.headers as Record<string, string>;
    }
    if (options.abortController != null) {
        requestOptions.abort = options.abortController.signal;
    }

    return requestOptions;
}

import {useAppDispatch} from './use-app-dispatch';
import {useCallback, useEffect, useRef} from 'react';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {useAppSelector} from './use-app-selector';
import {clearAuthData, selectAuthData, setAuthData} from '../slices/auth-slice';
import {ApiError} from '../models/api-error';
import {ApiOptions, ApiService} from '../services/api-service';
import {Api} from '@mui/icons-material';

export interface Api {
    isAuthenticated(): boolean;

    get<T>(url: string, options?: ApiOptions): Promise<T>;

    getPublic<T>(url: string, options?: ApiOptions): Promise<T>;

    getBlob(url: string, options?: ApiOptions): Promise<Blob>;

    post<T>(url: string, data: any, options?: ApiOptions): Promise<T>;

    postFormData<T>(url: string, data: FormData, options?: ApiOptions): Promise<T>;

    postFormUrlEncoded<T>(url: string, data: Record<string, string>, options?: ApiOptions): Promise<T>;

    put<T>(url: string, data: any, options?: ApiOptions): Promise<T>;

    destroy<T>(url: string, options?: ApiOptions): Promise<void>;
}

export function useApi(): Api {
    const dispatch = useAppDispatch();
    const authData = useAppSelector(selectAuthData);

    const serviceRef = useRef<ApiService>(new ApiService());

    useEffect(() => {
        if (authData != null) {
            serviceRef.current = new ApiService({
                authData: authData,
                onAuthDataChange: (authData: AuthDataDto) => {
                    dispatch(setAuthData(authData));
                },
            });
        } else {
            serviceRef.current = new ApiService();
        }
    }, [authData]);

    const isAuthenticated = useCallback(() => {
        return serviceRef.current.isAuthenticated() ?? false;
    }, []);

    const get = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return await serviceRef.current.get(`/api/${url}`, undefined, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const getPublic = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return serviceRef.current.get(`/api/public/${url}`, undefined, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const getBlob = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return await serviceRef.current.getBlob(`/api/${url}`, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const post = useCallback<any>(async (url: string, data: any, options?: ApiOptions) => {
        try {
            return await serviceRef.current.post(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const postFormData = useCallback<any>(async (url: string, data: FormData, options?: ApiOptions) => {
        try {
            return await serviceRef.current.postFormData(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const postFormUrlEncoded = useCallback<any>(async (url: string, data: Record<string, string>, options?: ApiOptions) => {
        try {
            return await serviceRef.current.postFormUrlEncoded(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const put = useCallback<any>(async (url: string, data: any, options?: ApiOptions) => {
        try {
            return await serviceRef.current.put(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    const destroy = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return await serviceRef.current.delete(`/api/${url}`, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
                return;
            }
            throw err;
        }
    }, []);

    return {
        isAuthenticated,
        get,
        getPublic,
        getBlob,
        post,
        postFormData,
        postFormUrlEncoded,
        put,
        destroy,
    };
}

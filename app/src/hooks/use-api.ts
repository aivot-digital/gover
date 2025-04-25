import {useAppDispatch} from './use-app-dispatch';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {useAppSelector} from './use-app-selector';
import {clearAuthData, selectAuthData, setAuthData} from '../slices/auth-slice';
import {isApiError} from '../models/api-error';
import {ApiOptions, ApiService} from '../services/api-service';
import {Api} from '@mui/icons-material';

export interface Api {
    isAuthenticated: boolean;

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
        return {
            isAuthenticated: isAuthenticated,
            get: async <T>(url: string, options?: ApiOptions): Promise<T> => {
                try {
                    return await serviceRef
                        .current
                        .get<T>(`/api/${url}`, undefined, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            getPublic: async <T>(url: string, options?: ApiOptions): Promise<T> => {
                try {
                    return await serviceRef
                        .current
                        .get<T>(`/api/public/${url}`, undefined, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            getBlob: async (url: string, options?: ApiOptions): Promise<Blob> => {
                try {
                    return await serviceRef
                        .current
                        .getBlob(`/api/${url}`, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            post: async <T>(url: string, data: any, options?: ApiOptions): Promise<T> => {
                try {
                    return await serviceRef
                        .current
                        .post(`/api/${url}`, data, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            postFormData: async <T>(url: string, data: FormData, options?: ApiOptions): Promise<T> => {
                try {
                    return await serviceRef
                        .current
                        .postFormData(`/api/${url}`, data, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            postFormUrlEncoded: async <T>(url: string, data: Record<string, string>, options?: ApiOptions): Promise<T> => {
                try {
                    return await serviceRef
                        .current
                        .postFormUrlEncoded(`/api/${url}`, data, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            put: async <T>(url: string, data: any, options?: ApiOptions): Promise<T> => {
                try {
                    return await serviceRef
                        .current
                        .put(`/api/${url}`, data, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
            destroy: async <T>(url: string, options?: ApiOptions): Promise<void> => {
                try {
                    return await serviceRef
                        .current
                        .delete(`/api/${url}`, options);
                } catch (err) {
                    throw handleUnauthorized(err);
                }
            },
        };
    }, [isAuthenticated]);
}

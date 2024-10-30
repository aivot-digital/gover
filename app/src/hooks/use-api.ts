import {useAppDispatch} from './use-app-dispatch';
import {useCallback, useMemo} from 'react';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {useAppSelector} from './use-app-selector';
import {clearAuthData, setAuthData} from '../slices/auth-slice';
import {ApiError} from "../models/api-error";
import {ApiOptions, ApiService} from '../services/api-service';
import {Api} from "@mui/icons-material";
import {AuthData} from '../models/dtos/auth-data';



export interface Api {
    getAuthData(): AuthData | undefined;

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
    const authState = useAppSelector(state => state.auth);

    const service = useMemo(() => authState.authData != null ? new ApiService({
        authData: authState.authData,
        onAuthDataChange: (authData: AuthDataDto) => {
            dispatch(setAuthData(authData));
        },
    }) : new ApiService(), [authState, dispatch]);

    const getAuthData = useCallback(() => {
        return service?.getAuthData();
    }, [service]);

    const isAuthenticated = useCallback(() => {
        return service?.isAuthenticated() ?? false;
    }, [service]);

    const get = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return await service?.get(`/api/${url}`, undefined, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const getPublic = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return service?.get(`/api/public/${url}`, undefined, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const getBlob = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return await service?.getBlob(`/api/${url}`, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const post = useCallback<any>(async (url: string, data: any, options?: ApiOptions) => {
        try {
            return await service?.post(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const postFormData = useCallback<any>(async (url: string, data: FormData, options?: ApiOptions) => {
        try {
            return await service?.postFormData(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const postFormUrlEncoded = useCallback<any>(async (url: string, data: Record<string, string>, options?: ApiOptions) => {
        try {
            return await service?.postFormUrlEncoded(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const put = useCallback<any>(async (url: string, data: any, options?: ApiOptions) => {
        try {
            return await service?.put(`/api/${url}`, data, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const destroy = useCallback<any>(async (url: string, options?: ApiOptions) => {
        try {
            return await service?.delete(`/api/${url}`, options);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    return useMemo(() => ({
        getAuthData,
        isAuthenticated,
        get,
        getPublic,
        getBlob,
        post,
        postFormData,
        postFormUrlEncoded,
        put,
        destroy,
    }), [
        getAuthData,
        isAuthenticated,
        get,
        getPublic,
        getBlob,
        post,
        postFormData,
        postFormUrlEncoded,
        put,
        destroy,
    ]);
}

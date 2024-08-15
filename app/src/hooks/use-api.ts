import {useAppDispatch} from './use-app-dispatch';
import {useCallback, useMemo} from 'react';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {useAppSelector} from './use-app-selector';
import {clearAuthData, setAuthData} from '../slices/auth-slice';
import {ApiError} from "../models/api-error";
import {ApiService} from "../services/api-service";
import {Api} from "@mui/icons-material";
import {AuthData} from '../models/dtos/auth-data';

type FilterValue = string | number | boolean | undefined | null;

type Filter = Record<string, FilterValue | Array<FilterValue>>;

export interface Api {
    getAuthData(): AuthData | undefined;

    isAuthenticated(): boolean;

    get<T>(url: string, filter?: Filter): Promise<T>;

    getPublic<T>(url: string, filter?: Filter): Promise<T>;

    getBlob(url: string): Promise<Blob>;

    post<T>(url: string, data: any): Promise<T>;

    postFormData<T>(url: string, data: FormData): Promise<T>;

    postFormUrlEncoded<T>(url: string, data: Record<string, string>): Promise<T>;

    put<T>(url: string, data: any): Promise<T>;

    destroy<T>(url: string): Promise<void>;
}

function makeQuery(filter?: Filter): string {
    if (filter == null) {
        return '';
    }

    const searchParams = new URLSearchParams();

    for (const [key, value] of Object.entries(filter)) {
        if (value != null) {
            if (typeof value === 'boolean') {
                searchParams.append(key, value ? 'true' : 'false');
            } if (Array.isArray(value)) {
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

    return searchParams.toString();
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

    const get = useCallback<any>(async (url: string, filter?: Filter) => {
        const query = makeQuery(filter);
        try {
            return await service?.get(`/api/${url}?${query.toString()}`);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const getPublic = useCallback<any>(async (url: string, filter?: Filter) => {
        const query = makeQuery(filter);
        try {
            return service?.get(`/api/public/${url}?${query.toString()}`);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const getBlob = useCallback<any>(async (url: string) => {
        try {
            return await service?.getBlob(`/api/${url}`);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const post = useCallback<any>(async (url: string, data: any) => {
        try {
            return await service?.post(`/api/${url}`, data);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const postFormData = useCallback<any>(async (url: string, data: FormData) => {
        try {
            return await service?.postFormData(`/api/${url}`, data);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const postFormUrlEncoded = useCallback<any>(async (url: string, data: Record<string, string>) => {
        try {
            return await service?.postFormUrlEncoded(`/api/${url}`, data);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const put = useCallback<any>(async (url: string, data: any) => {
        try {
            return await service?.put(`/api/${url}`, data);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                dispatch(clearAuthData());
            }
            throw err;
        }
    }, [service]);

    const destroy = useCallback<any>(async (url: string) => {
        try {
            return await service?.delete(`/api/${url}`);
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

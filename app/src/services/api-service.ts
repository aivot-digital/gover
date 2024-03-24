import {AuthDataDto} from "../models/dtos/auth-data-dto";
import {ApiError} from "../models/api-error";
import {AuthData} from "../models/dtos/auth-data";
import {AppConfig} from '../app-config';

export type FilterValue = string | number | boolean | undefined | null;
export type Filter = Record<string, FilterValue | Array<FilterValue>>;

interface Auth {
    authData: AuthData;
    onAuthDataChange: (authData: AuthDataDto) => void;
}

export class ApiService {
    protected readonly auth: Auth | undefined;

    public constructor(auth?: Auth) {
        this.auth = auth;
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

    public isNotAuthenticated() {
        return (
            this.auth == null ||
            this.auth.authData == null ||
            this.auth.authData.refreshToken == null  ||
            this.auth.authData.refreshToken.expires <= Date.now()
        );
    }

    protected async getAccessToken(): Promise<string | undefined> {
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
        });

        if (response.status !== 200) {
            return undefined;
        }

        const refreshedAuthData: AuthDataDto = await response.json();
        onAuthDataChange(refreshedAuthData);
        return refreshedAuthData.access_token;
    }

    public async get<T>(url: string, headers?: any): Promise<T> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                ...(accessToken != null ? {
                    Authorization: `Bearer ${accessToken}`,
                } : undefined),
                ...headers,
            },
        });
        if (response.status !== 200) {
            throw new ApiError(response.status, await response.text());
        }
        return await response.json();
    }

    public async getBlob(url: string): Promise<Blob> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/octet-stream',
                ...(accessToken != null ? {
                    Authorization: `Bearer ${accessToken}`,
                } : undefined)
            },
        });
        if (response.status !== 200) {
            throw new ApiError(response.status, await response.text());
        }
        return await response.blob();
    }

    public async post<T>(url: string, data: any): Promise<T> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'POST',
            body: JSON.stringify(data),
            headers: {
                'Content-Type': 'application/json',
                ...(accessToken != null ? {
                    Authorization: `Bearer ${accessToken}`,
                } : undefined)
            },
        });
        if (response.status !== 200 && response.status !== 201) {
            throw new ApiError(response.status, await response.text());
        }
        return await response.json();
    }

    public async postFormData<T>(url: string, data: FormData): Promise<T> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'POST',
            body: data,
            headers: accessToken != null ? {
                Authorization: `Bearer ${accessToken}`,
            } : undefined,
        });
        if (response.status !== 200 && response.status !== 201) {
            throw new ApiError(response.status, await response.text());
        }
        return await response.json();
    }

    public async postFormUrlEncoded<T>(url: string, data: Record<string, string>): Promise<T> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'POST',
            body: new URLSearchParams(data),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                ...(accessToken != null ? {
                    Authorization: `Bearer ${accessToken}`,
                } : undefined)
            },
        });
        if (response.status !== 200 && response.status !== 201) {
            throw new ApiError(response.status, await response.text());
        }
        return await response.json();
    }

    public async put<T>(url: string, data: any): Promise<T> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'PUT',
            body: JSON.stringify(data),
            headers: {
                'Content-Type': 'application/json',
                ...(accessToken != null ? {
                    Authorization: `Bearer ${accessToken}`,
                } : undefined)
            },
        });
        if (response.status !== 200) {
            throw new ApiError(response.status, await response.text());
        }
        return await response.json();
    }

    public async delete(url: string): Promise<void> {
        const accessToken = await this.getAccessToken();
        const response = await window.fetch(url, {
            method: 'DELETE',
            headers: accessToken != null ? {
                'Authorization': `Bearer ${accessToken}`,
            } : undefined,
        });
        if (response.status !== 200 && response.status !== 204) {
            throw new ApiError(response.status, await response.text());
        }
    }
}
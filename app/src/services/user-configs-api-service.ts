import { Api } from '../hooks/use-api';

export interface PaginationResponse<T> {
    content: T[];
    pageable: any;
    last: boolean;
    totalElements: number;
    totalPages: number;
    first: boolean;
    size: number;
    number: number;
    sort: {
        empty: boolean;
        sorted: boolean;
        unsorted: boolean;
    };
    numberOfElements: number;
    empty: boolean;
}

export interface UserConfigDefinition {
    hiddenInUI: boolean;
    key: string;
    label: string;
    category: string;
    subCategory: string;
    description: string;
    package: string;
    isPublicConfig: boolean;
    type: 'ASSET' | 'FLAG' | 'TEXT' | 'LIST';
    options: any[];
    defaultValue: any[];
}

export interface UserConfig {
    userId: string;
    key: string;
    value: string;
}

export class UserConfigsApiService {
    private readonly api: Api;

    constructor(api: Api) {
        this.api = api;
    }

    public async listDefinitions(page: number = 0, size: number = 10): Promise<PaginationResponse<UserConfigDefinition>> {
        return await this.api.get<PaginationResponse<UserConfigDefinition>>(`user-config-definitions/?page=${page}&size=${size}`, {});
    }

    public async list(userId: string | 'self', page: number = 0, size: number = 10): Promise<PaginationResponse<UserConfig>> {
        return await this.api.get<PaginationResponse<UserConfig>>(`user-configs/${userId}/?page=${page}&size=${size}`, {});
    }

    public async retrieve(userId: string | 'self', key: string): Promise<UserConfig> {
        return await this.api.get<UserConfig>(`user-configs/${userId}/${key}/`, {});
    }

    public async save(userId: string | 'self', key: string, value: string | boolean | string[]): Promise<UserConfig> {
        return await this.api.put<UserConfig>(`user-configs/${userId}/${key}/`, {
            value: typeof value === 'boolean' ? (value ? 'true' : '') : value,
        });
    }
}
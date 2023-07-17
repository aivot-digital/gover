import {ApiConfig} from '../api-config';
import {LocalStorageService} from './local-storage-service';
import {LocalstorageKey} from '../data/localstorage-key';

export class ApiError extends Error {
    constructor(public readonly status: number, public readonly details: any) {
        super('api error', details);
    }
}

export class ApiService<T, L, I> {
    private static readonly basePath = `${ApiConfig.address}`;

    protected readonly path: string;

    public constructor(path: string) {
        this.path = path;
    }

    public static async get<R>(path: string): Promise<R> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, ApiService.getConfig());
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
        return await res.json();
    }

    public static async getBlob(path: string): Promise<Blob> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, ApiService.getConfig());
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
        return await res.blob();
    }

    public static async post<R>(path: string, data: any): Promise<R> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, {
            method: 'POST',
            body: JSON.stringify(data),
            ...ApiService.getConfig(),
        });
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
        return await res.json();
    }

    public static async postFormData<R>(path: string, data: FormData): Promise<R> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, {
            method: 'POST',
            body: data,
            headers: {
                Authorization: ApiService.getConfig().headers.Authorization,
            },
        });
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
        return await res.json();
    }

    public static async put<R>(path: string, data: any): Promise<R> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, {
            method: 'PUT',
            body: JSON.stringify(data),
            ...ApiService.getConfig(),
        });
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
        return await res.json();
    }

    public static async patch<R>(path: string, data: any): Promise<R> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, {
            method: 'PATCH',
            body: JSON.stringify(data),
            ...ApiService.getConfig(),
        });
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
        return await res.json();
    }

    public static async delete(path: string): Promise<void> {
        const res = await window.fetch(`${ApiService.basePath}/${path}`, {
            method: 'DELETE',
            ...ApiService.getConfig(),
        });
        if (res.status !== 200) {
            throw new ApiError(res.status, await res.json());
        }
    }

    public async list(filter?: Record<string, string | number>): Promise<L[]> {
        const queryParams = [];
        if (filter != null) {
            for (const key of Object.keys(filter)) {
                queryParams.push(`${key}=${filter[key]}`);
            }
        }

        return await ApiService.get(`${this.path}?${queryParams.join('&')}`);
    }

    public async retrieve(id: I): Promise<T> {
        return await ApiService.get(this.path + '/' + id);
    }

    public async create(data: T): Promise<L> {
        return await ApiService.post(this.path, data);
    }

    public async update(id: I, data: T): Promise<T> {
        return await ApiService.put(this.path + '/' + id, data);
    }

    public async destroy(id: I): Promise<void> {
        await ApiService.delete(this.path + '/' + id);
    }

    public async save(id: I | undefined, data: T): Promise<T | L> {
        if (id == null) {
            return await this.create(data);
        } else {
            return await this.update(id, data);
        }
    }

    private static getConfig(): any {
        const jwt = LocalStorageService.loadString(LocalstorageKey.JWT);
        return {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': jwt != null ? `Bearer ${jwt}` : undefined,
            },
        };
    }
}

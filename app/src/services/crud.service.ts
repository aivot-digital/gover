import {ApiConfig} from '../api-config';
import axios, {AxiosRequestConfig} from 'axios';
import {ApiListResponse} from '../models/api-list-response';
import {ApiDetailsResponse} from '../models/api-details-response';
import {LocalStorageService} from './local-storage.service';
import {LocalstorageKey} from "../data/localstorage-key";

export class CrudService<T extends { id: number }, A extends string, I> {
    protected readonly basePath: string;
    protected readonly path: string;

    public constructor(path: string) {
        this.basePath = `${ApiConfig.address}/`;
        this.path = `${ApiConfig.address}/${path}`;
    }

    list(): Promise<ApiListResponse<T, A>> {
        return axios.get(this.path + '?size=500', CrudService.getConfig())
            .then(response => response.data);
    }

    retrieve(id: I): Promise<ApiDetailsResponse<T>> {
        return axios.get(this.path + '/' + id, CrudService.getConfig())
            .then(response => response.data);
    }

    create(data: Omit<T, 'id'>): Promise<ApiDetailsResponse<T>> {
        return axios.post(this.path, data, CrudService.getConfig())
            .then(response => response.data);
    }

    update(id: I, data: T): Promise<ApiDetailsResponse<T>> {
        return axios.put(this.path + '/' + id, data, CrudService.getConfig())
            .then(response => response.data);
    }

    destroy(id: I): Promise<void> {
        return axios.delete(this.path + '/' + id, CrudService.getConfig());
    }

    public static getConfig(): AxiosRequestConfig {
        const jwt = LocalStorageService.loadString(LocalstorageKey.JWT);
        return {
            timeout: 1000,
            headers: jwt != null ? {
                Authorization: `Bearer ${jwt}`,
            } : undefined,
        };
    }
}

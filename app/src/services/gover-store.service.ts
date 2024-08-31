import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type StoreListResponse} from '../models/entities/store-list-response';
import {type StoreListModule} from '../models/entities/store-list-module';
import {type StoreDetailModule} from '../models/entities/store-detail-module';
import {type StoreCreateModule} from '../models/entities/store-create-module';
import {type StoreUpdateModule} from '../models/entities/store-update-module';

import {ApiError} from "../models/api-error";

export const storeBaseUrl = 'https://store.gover.digital/api/';

class _GoverStoreService {
    async listModules(page: number, search?: string, key?: string): Promise<StoreListResponse<StoreListModule>> {
        const resp = await fetch(
            `${storeBaseUrl}modules/?page=${page}&size=999&search=${search ?? ''}`,
            key != null ?
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': key,
                    },
                } :
                undefined,
        );
        if (resp.status !== 200) {
            throw new ApiError(resp.status, await resp.json());
        }
        return await resp.json();
    }

    async fetchModule(id: string, key?: string): Promise<StoreDetailModule> {
        const resp = await fetch(
            `${storeBaseUrl}modules/${id}/`,
            key != null ?
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': key,
                    },
                } :
                undefined,
        );
        if (resp.status !== 200) {
            throw new ApiError(resp.status, await resp.json());
        }
        return await resp.json();
    }

    async fetchModuleCode(id: string, version: string, key: string | undefined): Promise<GroupLayout> {
        const resp = await fetch(
            `${storeBaseUrl}modules/${id}/${version}/`,
            key != null ?
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': key,
                    },
                } :
                undefined,
        );
        if (resp.status !== 200) {
            throw new ApiError(resp.status, await resp.json());
        }
        return await resp.json();
    }

    async publishModule(
        key: string,
        module: StoreCreateModule,
    ): Promise<StoreDetailModule> {
        const resp = await fetch(
            `${storeBaseUrl}modules/`,
            {
                method: 'POST',
                body: JSON.stringify(module),
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': key,
                },
            },
        );
        if (resp.status !== 200 && resp.status !== 201) {
            throw new ApiError(resp.status, await resp.json());
        }
        return await resp.json();
    }

    async publishModuleVersion(
        key: string,
        moduleId: string,
        module: StoreUpdateModule,
    ): Promise<StoreDetailModule> {
        const resp = await fetch(
            `${storeBaseUrl}modules/${moduleId}/`,
            {
                method: 'PATCH',
                body: JSON.stringify(module),
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': key,
                },
            },
        );
        if (resp.status !== 200 && resp.status !== 201) {
            throw new ApiError(resp.status, await resp.json());
        }
        return await resp.json();
    }
}


export const GoverStoreService = new _GoverStoreService();

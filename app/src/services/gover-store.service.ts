import {GroupLayout} from "../models/elements/form/layout/group-layout";
import {StoreListResponse} from "../models/entities/store-list-response";
import {StoreListForm} from "../models/entities/store-list-form";
import {StoreCreateForm} from "../models/entities/store-create-form";
import {StoreListModule} from "../models/entities/store-list-module";
import {StoreDetailModule} from "../models/entities/store-detail-module";
import {StoreCreateModule} from "../models/entities/store-create-module";

export const storeBaseUrl = 'https://store.gover.digital/api/';

class _GoverStoreService {
    async listForms(page: number, search?: string, key?: string): Promise<StoreListResponse<StoreListForm>> {
        const resp = await fetch(
            `${storeBaseUrl}forms/?page=${page}&size=20&search=${search ?? ''}`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined,
        );
        return resp.json();
    }

    async publishForm(
        key: string,
        form: StoreCreateForm,
    ): Promise<any> {
        const resp = await fetch(
            `${storeBaseUrl}forms/`,
            {
                method: 'POST',
                body: JSON.stringify(form),
                headers: {
                    Authorization: key,
                },
            },
        );
        return resp.json();
    }

    async listModules(page: number, search?: string, key?: string): Promise<StoreListResponse<StoreListModule>> {
        const resp = await fetch(
            `${storeBaseUrl}modules/?page=${page}&size=999&search=${search ?? ''}`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined,
        );
        return resp.json();
    }

    async fetchModule(id: string, key?: string): Promise<StoreDetailModule> {
        const resp = await fetch(
            `${storeBaseUrl}modules/${id}/`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined,
        );
        return await resp.json();
    }

    async fetchModuleCode(id: string, version: string, key: string | undefined): Promise<GroupLayout> {
        const resp = await fetch(
            `${storeBaseUrl}modules/${id}/${version}/`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined,
        );
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
                    Authorization: key,
                },
            },
        );
        return resp.json();
    }
}


export const GoverStoreService = new _GoverStoreService();

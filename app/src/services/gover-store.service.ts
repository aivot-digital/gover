import axios from 'axios';
import {CrudService} from './crud.service';
import {User} from "../models/entities/user";
import {RootElement} from "../models/elements/root-element";
import {GroupLayout} from "../models/elements/form/layout/group-layout";

export const storeBaseUrl = 'https://store.gover.digital/api/';

class _GoverStoreService {
    async listForms(page: number, search: string | undefined, key: string | undefined): Promise<any> {
        const resp = await axios.get(
            `${storeBaseUrl}forms/?page=${page}&size=20&search=${search ?? ''}`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined
        );
        return resp.data;
    }

    async publishForm(
        key: string,
        root: RootElement,
        version: string,
        title: string,
        description: string,
        descriptionShort: string,
        isPublic: boolean,
        leikaIds: string[],
    ): Promise<any> {
        const resp = await axios.post(
            storeBaseUrl + 'forms/',
            {
                version: version,
                title: title,
                description: description,
                description_short: descriptionShort,
                is_public: isPublic,
                leika_ids: leikaIds,
                gover_root: root,
            },
            {
                headers: {
                    Authorization: key,
                },
            }
        );
        return resp.data;
    }

    async listModules(page: number, search: string | undefined, key: string | undefined): Promise<ListModule[]> {
        const resp = await axios.get(
            `${storeBaseUrl}modules/?page=${page}&size=999&search=${search ?? ''}`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined
        );
        return resp.data.items;
    }

    async fetchModule(id: string, key: string | undefined): Promise<DetailModule> {
        const resp = await axios.get(
            `${storeBaseUrl}modules/${id}/`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined
        );
        return resp.data;
    }

    async fetchModuleCode(id: string, version: string, key: string | undefined): Promise<GroupLayout> {
        const resp = await axios.get(
            `${storeBaseUrl}modules/${id}/${version}/`,
            key != null ? {
                headers: {
                    Authorization: key,
                },
            } : undefined
        );
        return resp.data;
    }

    async publishModule(
        key: string,
        root: GroupLayout,
        version: string,
        title: string,
        description: string,
        descriptionShort: string,
        isPublic: boolean,
        datenfeldId: string,
    ): Promise<DetailModule> {
        const resp = await axios.post(
            storeBaseUrl + 'modules/',
            {
                version: version,
                title: title,
                description: description,
                description_short: descriptionShort,
                is_public: isPublic,
                datenfeld_id: datenfeldId,
                gover_root: root,
            },
            {
                headers: {
                    Authorization: key,
                },
            }
        );
        return resp.data;
    }
}


export const GoverStoreService = new _GoverStoreService();

export interface ListModule {
    current_version: string;
    datenfeld_id: string;
    description_short: string;
    id: string;
    is_public: boolean;
    organization: string;
    organization_id: string;
    title: string;
}

export interface DetailModule {
    id: string;
    organization: string;
    organization_id: string;
    title: string;
    description_short: string;
    is_public: boolean;
    current_version: string;
    datenfeld_id: string;
    description: string;
    recent_changes: string;
    versions: string[];
}
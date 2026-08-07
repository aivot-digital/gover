import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type MarketplaceListResponse} from '../models/entities/marketplace-list-response';
import {type MarketplaceListModule} from '../models/entities/marketplace-list-module';
import {type MarketplaceDetailModule} from '../models/entities/marketplace-detail-module';
import {type MarketplaceCreateModule} from '../models/entities/marketplace-create-module';
import {type MarketplaceUpdateModule} from '../models/entities/marketplace-update-module';

import {createApiError} from '../models/api-error';

export const marketplaceBaseUrl = 'https://marketplace.prosuna.de/api/';

class _ProsunaMarketplaceService {
    async listModules(page: number, search?: string, key?: string): Promise<MarketplaceListResponse<MarketplaceListModule>> {
        const resp = await fetch(
            `${marketplaceBaseUrl}modules/?page=${page}&size=999&search=${search ?? ''}`,
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
            throw await createApiError(resp);
        }
        return await resp.json();
    }

    async fetchModule(id: string, key?: string): Promise<MarketplaceDetailModule> {
        const resp = await fetch(
            `${marketplaceBaseUrl}modules/${id}/`,
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
            throw await createApiError(resp);
        }
        return await resp.json();
    }

    async fetchModuleCode(id: string, version: string, key: string | undefined): Promise<GroupLayout> {
        const resp = await fetch(
            `${marketplaceBaseUrl}modules/${id}/${version}/`,
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
            throw await createApiError(resp);
        }
        return await resp.json();
    }

    async publishModule(
        key: string,
        module: MarketplaceCreateModule,
    ): Promise<MarketplaceDetailModule> {
        const resp = await fetch(
            `${marketplaceBaseUrl}modules/`,
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
            throw await createApiError(resp);
        }
        return await resp.json();
    }

    async publishModuleVersion(
        key: string,
        moduleId: string,
        module: MarketplaceUpdateModule,
    ): Promise<MarketplaceDetailModule> {
        const resp = await fetch(
            `${marketplaceBaseUrl}modules/${moduleId}/`,
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
            throw await createApiError(resp);
        }
        return await resp.json();
    }
}


export const ProsunaMarketplaceService = new _ProsunaMarketplaceService();

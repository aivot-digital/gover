import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {ProviderLinkRequestDTO} from './dtos/provider-link-request-dto';
import {ProviderLinkResponseDTO} from './dtos/provider-link-response-dto';
import {Page} from '../../models/dtos/page';
import {SortOrder} from '../../components/generic-list/generic-list-props';

interface ProviderLinkFilters {
    text: string;
}

export class ProviderLinksApiService extends CrudApiService<ProviderLinkRequestDTO, ProviderLinkResponseDTO, ProviderLinkResponseDTO, ProviderLinkResponseDTO, ProviderLinkResponseDTO, number, ProviderLinkFilters> {
    public constructor(api: Api) {
        super(api, 'provider-links/');
    }

    public initialize(): ProviderLinkResponseDTO {
        return {
            id: 0,
            text: '',
            link: '',
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }

    public async listAvailable(
        page: number,
        limit: number,
        sort?: keyof ProviderLinkResponseDTO,
        order?: SortOrder,
        filters?: Partial<ProviderLinkFilters>,
    ): Promise<Page<ProviderLinkResponseDTO>> {
        return await this.api.get<Page<ProviderLinkResponseDTO>>(`${this.path}available/`, {
            queryParams: {
                page,
                size: limit,
                sort: sort != null && order != null ? `${sort},${order}` : undefined,
                ...filters,
            },
        });
    }

    public async listAvailableOrdered(
        sort?: keyof ProviderLinkResponseDTO,
        order?: SortOrder,
        filters?: Partial<ProviderLinkFilters>,
    ): Promise<Page<ProviderLinkResponseDTO>> {
        return await this.listAvailable(0, 999, sort, order, filters);
    }
}

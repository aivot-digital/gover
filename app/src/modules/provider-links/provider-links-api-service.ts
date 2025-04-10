import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {ProviderLinkRequestDTO} from './dtos/provider-link-request-dto';
import {ProviderLinkResponseDTO} from './dtos/provider-link-response-dto';

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
}
import {Api} from '../../hooks/use-api';
import {CrudApiService} from '../../services/crud-api-service';
import {IdentityProviderDetailsDTO} from './models/identity-provider-details-dto';
import {IdentityProviderListDTO} from './models/identity-provider-list-dto';
import {IdentityProviderRequestDTO} from './models/identity-provider-request-dto';
import {IdentityProviderType} from './enums/identity-provider-type';
import {IdentityProviderInfo} from './models/identity-provider-info';
import {IdentityData} from './models/identity-data';
import {IdentityIdHeader} from './constants/identity-id-header';

export interface IdentityProvidersFilter {
    keys: string[];
    name: string;
    iconAssetKey: string;
    clientSecretKey: string;
    type: IdentityProviderType;
    isEnabled: boolean;
    isTestProvider: boolean;
}

export class IdentityProvidersApiService extends CrudApiService<IdentityProviderRequestDTO, IdentityProviderListDTO, IdentityProviderListDTO, IdentityProviderDetailsDTO, IdentityProviderDetailsDTO, string, IdentityProvidersFilter> {
    constructor(api: Api) {
        super(api, 'identity-providers/');
    }

    public initialize(): IdentityProviderDetailsDTO {
        return {
            key: '',
            type: IdentityProviderType.Custom,
            metadataIdentifier: '',
            additionalParams: [],
            attributes: [],
            clientId: '',
            clientSecretKey: undefined,
            description: '',
            authorizationEndpoint: '',
            name: '',
            tokenEndpoint: '',
            userinfoEndpoint: undefined,
            endSessionEndpoint: undefined,
            isEnabled: false,
            iconAssetKey: undefined,
            defaultScopes: [],
            isTestProvider: false,
        };
    }

    public async prepare(endpoint: string): Promise<IdentityProviderDetailsDTO> {
        return await this.api.post<IdentityProviderDetailsDTO>('identity-providers/prepare/', {
            endpoint: endpoint,
        });
    }

    public static createLink(key: string, additionalScopes?: string[]): string {
        return '/api/public/identity/' + key + '/start/' + (additionalScopes != null ? '?additionalScopes=' + additionalScopes.join('%20') : '');
    }

    public static async fetchIdentity(identityId: string): Promise<IdentityData> {
        const res = await fetch('/api/public/identity/get/', {
            headers: {
                [IdentityIdHeader]: identityId,
            },
        });
        return await res.json();
    }
}

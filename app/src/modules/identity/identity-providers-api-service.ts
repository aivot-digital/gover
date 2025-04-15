import {Api} from '../../hooks/use-api';
import {CrudApiService} from '../../services/crud-api-service';
import {IdentityProviderDetailsDTO} from './models/identity-provider-details-dto';
import {IdentityProviderListDTO} from './models/identity-provider-list-dto';
import {IdentityProviderRequestDTO} from './models/identity-provider-request-dto';
import {IdentityProviderType} from './enums/identity-provider-type';
import {IdentityProviderInfo} from './models/identity-provider-info';

export interface IdentityProvidersFilter {
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
            isTestProvider: false
        };
    }

    public async prepare(endpoint: string): Promise<IdentityProviderDetailsDTO> {
        return await this.api.post<IdentityProviderDetailsDTO>('identity-providers/prepare/', {
            endpoint: endpoint,
        });
    }

    public static async getIdentityProviderInfo(key: string): Promise<IdentityProviderInfo> {
        const res = await fetch(`/api/public/identity/${key}/info/`);
        return await res.json();
    }

    public static createLink(key: string, additionalScopes?: string): string {
        return '/api/public/identity/' + key + '/start/' + (additionalScopes != null ? '&additionalScopes=' + additionalScopes : '');
    }

    public static async fetchIdentity(): Promise<Record<string, string>> {
        const res = await fetch('/api/public/identity/get/');
        return await res.json();
    }
}

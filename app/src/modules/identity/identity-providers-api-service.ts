import {IdentityProviderDetailsDTO} from './models/identity-provider-details-dto';
import {IdentityProviderListDTO} from './models/identity-provider-list-dto';
import {IdentityProviderRequestDTO} from './models/identity-provider-request-dto';
import {IdentityProviderType} from './enums/identity-provider-type';
import {IdentityDataMap} from './models/identity-data';
import {createApiPath} from '../../utils/url-path-utils';
import {BaseCrudApiService} from '../../services/base-crud-api-service';

export interface IdentityProvidersFilter {
    keys: string[];
    name: string;
    iconAssetKey: string;
    clientSecretKey: string;
    type: IdentityProviderType;
    isEnabled: boolean;
    isTestProvider: boolean;
}

export class IdentityProvidersApiService extends BaseCrudApiService<
    IdentityProviderRequestDTO,
    IdentityProviderListDTO,
    IdentityProviderDetailsDTO,
    IdentityProviderRequestDTO,
    string,
    IdentityProvidersFilter
> {
    constructor() {
        super('/api/identity-providers/');
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
            iconAssetKey: null,
            defaultScopes: [],
            isTestProvider: false,
        };
    }

    public async prepare(endpoint: string): Promise<IdentityProviderDetailsDTO> {
        return await this.post<any, IdentityProviderDetailsDTO>('/api/identity-providers/prepare/', {
            endpoint: endpoint,
        });
    }

    public static createLink(key: string, identityId: string, additionalScopes?: string[], origin?: string): string {
        const path = createApiPath('/api/public/identity/' + key + '/' + identityId + '/start/');

        const resolvedOrigin = origin ?? `${window.location.origin}${window.location.pathname}`;
        const additionalScopesParam = additionalScopes != null ? additionalScopes.join(' ') : '';

        const searchParams = new URLSearchParams();
        searchParams.set('origin', resolvedOrigin);
        if (additionalScopesParam) {
            searchParams.set('additionalScopes', additionalScopesParam);
        }

        return path + '?' + searchParams.toString();

        //return createApiPath('/api/public/identity/' + key + '/start/') + (additionalScopes != null ? '?additionalScopes=' + additionalScopes.join('%20') : '');
    }

    public static async fetchIdentity(clear: boolean, relatedNodeId: number | undefined): Promise<IdentityDataMap> {
        const qp = new URLSearchParams();
        if (clear) {
            qp.set('clear', 'true');
        }
        if (relatedNodeId != null) {
            qp.set('relatedProcessNodeId', relatedNodeId.toString());
        }

        const res = await fetch(createApiPath('api/public/identity/get/?' + qp.toString()));
        return await res.json();
    }

    public static async clearIdentity(relatedNodeId: number): Promise<void> {
        const qp = new URLSearchParams();
        qp.set('relatedProcessNodeId', relatedNodeId.toString());

        await fetch(createApiPath('api/public/identity/session/?' + qp.toString()), {
            method: 'DELETE',
        });
    }
}

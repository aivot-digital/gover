import {BaseApiService} from '../../services/base-api-service';
import {type SortOrder} from '../../components/generic-list/generic-list-props';
import {type Page} from '../../models/dtos/page';
import {
    CommunicationConfigurationLayout,
    CommunicationProvider,
    CommunicationProviderBinding,
    CommunicationProviderBindingRequest,
    CommunicationProviderDefinition,
    CommunicationProviderRequest,
} from './models';

export class CommunicationProvidersApiService extends BaseApiService {
    private readonly path = '/api/communication-providers/';

    public initializeProvider(): CommunicationProvider {
        return {
            id: 0,
            communicationProviderDefinitionKey: '',
            communicationProviderDefinitionVersion: 0,
            name: '',
            description: '',
            configuration: {},
            isEnabled: false,
            isTestProvider: false,
        };
    }

    public listProviders(): Promise<CommunicationProvider[]> {
        return this.get(this.path);
    }

    public async listProvidersPage(
        page: number,
        size: number,
        sort: keyof CommunicationProvider = 'name',
        order: SortOrder = 'ASC',
        search?: string,
    ): Promise<Page<CommunicationProvider>> {
        const providers = await this.listProviders();
        const normalizedSearch = search?.trim().toLocaleLowerCase() ?? '';
        const filteredProviders = normalizedSearch.length === 0
            ? providers
            : providers.filter(provider => (
                `${provider.name} ${provider.description}`
                    .toLocaleLowerCase()
                    .includes(normalizedSearch)
            ));
        const sortedProviders = [...filteredProviders].sort((left, right) => {
            const comparison = compareProviderValues(left[sort], right[sort]);
            return order === 'DESC' ? -comparison : comparison;
        });
        const start = page * size;
        const totalElements = sortedProviders.length;

        return {
            content: sortedProviders.slice(start, start + size),
            page: {
                size,
                number: page,
                totalElements,
                totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / size),
            },
        };
    }

    public retrieveProvider(id: number): Promise<CommunicationProvider> {
        return this.get(`${this.path}${id}/`);
    }

    public createProvider(request: CommunicationProviderRequest): Promise<CommunicationProvider> {
        return this.post(this.path, request);
    }

    public updateProvider(id: number, request: CommunicationProviderRequest): Promise<CommunicationProvider> {
        return this.put(`${this.path}${id}/`, request);
    }

    public deleteProvider(id: number): Promise<void> {
        return this.delete(`${this.path}${id}/`);
    }

    public listDefinitions(): Promise<CommunicationProviderDefinition[]> {
        return this.get(`${this.path}definitions/`);
    }

    public getProviderConfigurationLayout(definitionKey: string, version: number): Promise<CommunicationConfigurationLayout> {
        return this.get(`${this.path}definitions/configuration/`, {
            query: {definitionKey, version},
        });
    }

    public listBindings(identityProviderKey: string): Promise<CommunicationProviderBinding[]> {
        return this.get(`${this.path}bindings/`, {query: {identityProviderKey}});
    }

    public getBindingConfigurationLayout(communicationProviderId: number,
                                         identityProviderKey: string): Promise<CommunicationConfigurationLayout> {
        return this.get(`${this.path}bindings/configuration/`, {
            query: {communicationProviderId, identityProviderKey},
        });
    }

    public createBinding(request: CommunicationProviderBindingRequest): Promise<CommunicationProviderBinding> {
        return this.post(`${this.path}bindings/`, request);
    }

    public updateBinding(id: number,
                         request: CommunicationProviderBindingRequest): Promise<CommunicationProviderBinding> {
        return this.put(`${this.path}bindings/${id}/`, request);
    }

    public deleteBinding(id: number): Promise<void> {
        return this.delete(`${this.path}bindings/${id}/`);
    }
}

function compareProviderValues(left: unknown, right: unknown): number {
    if (left == null && right == null) return 0;
    if (left == null) return -1;
    if (right == null) return 1;

    if (typeof left === 'number' && typeof right === 'number') {
        return left - right;
    }
    if (typeof left === 'boolean' && typeof right === 'boolean') {
        return Number(left) - Number(right);
    }

    return String(left).localeCompare(String(right), 'de', {
        numeric: true,
        sensitivity: 'base',
    });
}

import {BaseApiService} from './base-api-service';

export enum PluginComponentType {
    JavascriptFunctionProvider = 'JavascriptFunctionProvider',
    StorageProviderDefinition = 'StorageProviderDefinition',
    ProcessNodeDefinition = 'ProcessNodeDefinition',
    PaymentProviderDefinition = 'PaymentProviderDefinition',
    OperatorProvider = 'OperatorProvider',
    CommunicationProviderDefinition = 'CommunicationProviderDefinition',
}

export const PluginComponentTypeOptions: PluginComponentType[] = [
    PluginComponentType.JavascriptFunctionProvider,
    PluginComponentType.StorageProviderDefinition,
    PluginComponentType.ProcessNodeDefinition,
    PluginComponentType.PaymentProviderDefinition,
    PluginComponentType.OperatorProvider,
    PluginComponentType.CommunicationProviderDefinition,
];

export const PluginComponentTypeDisplayNames: Record<PluginComponentType, string> = {
    [PluginComponentType.JavascriptFunctionProvider]: 'JavaScript-Funktionsanbieter',
    [PluginComponentType.StorageProviderDefinition]: 'Speicheranbieter-Definition',
    [PluginComponentType.ProcessNodeDefinition]: 'Prozessknoten-Definition',
    [PluginComponentType.PaymentProviderDefinition]: 'Zahlungsanbieter-Definition',
    [PluginComponentType.OperatorProvider]: 'Operator-Anbieter',
    [PluginComponentType.CommunicationProviderDefinition]: 'Kommunikationsanbieter-Definition',
};

export interface PluginDTO {
    key: string;
    name: string;
    description: string; // Markdown formatted description
    buildDate: string;
    version: string;
    vendorName: string;
    vendorWebsite: string;
    changelog: string; // Markdown formatted changelog
    deprecationNotice: null | string; // Markdown formatted deprecation notice, if applicable
    components: {
        parentPluginKey: string;
        componentKey: string;
        componentVersion: string;
        key: string;
        majorVersion: number;
        name: string;
        componentType: PluginComponentType;
        description: string; // Markdown formatted description
        deprecationNotice: null | string; // Markdown formatted deprecation notice, if applicable
    }[][];
}

export class PluginsApiService extends BaseApiService {
    public async getPlugins(): Promise<PluginDTO[]> {
        return this.get<PluginDTO[]>('/api/plugins/');
    }
}
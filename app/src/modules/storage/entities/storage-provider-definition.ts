import type {AnyLayoutElement} from '../../../models/elements/form/layout/any-layout-element';

export interface StorageProviderDefinition {
    key: string;
    version: number;
    name: string;
    abstractDescription: string;
    description: string;
    documentationUrl: string | null;
    providerConfigLayout: AnyLayoutElement | null;
    supportsMetadataAttributes: boolean;
}

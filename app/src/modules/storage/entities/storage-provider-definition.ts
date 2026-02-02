import type {AnyLayoutElement} from '../../../models/elements/form/layout/any-layout-element';

export interface StorageProviderDefinition {
    key: string;
    version: number;
    name: string;
    description: string;
    providerConfigLayout: AnyLayoutElement | null;
}

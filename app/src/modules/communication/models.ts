import {ConfigLayoutElement} from '../../models/elements/form/layout/config-layout-element';
import {IdentityProviderType} from '../identity/enums/identity-provider-type';

export interface CommunicationProvider {
    id: number;
    communicationProviderDefinitionKey: string;
    communicationProviderDefinitionVersion: number;
    name: string;
    description: string;
    configuration: Record<string, any>;
    isEnabled: boolean;
    isTestProvider: boolean;
}

export interface CommunicationProviderDefinition {
    key: string;
    version: number;
    name: string;
    description: string;
    supportedIdentityProviderTypes: IdentityProviderType[];
}

export interface CommunicationProviderBinding {
    id: number;
    identityProviderKey: string;
    communicationProviderId: number;
    name: string;
    description: string;
    isEnabled: boolean;
    position: number;
    configuration: Record<string, any>;
}

export type CommunicationProviderRequest = Omit<CommunicationProvider, 'id'>;
export type CommunicationProviderBindingRequest = Omit<CommunicationProviderBinding, 'id'>;
export type CommunicationConfigurationLayout = ConfigLayoutElement;

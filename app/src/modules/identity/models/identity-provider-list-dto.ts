import {IdentityProviderType} from '../enums/identity-provider-type';
import {IdentityAttributeMapping} from './identity-attribute-mapping';

export interface IdentityProviderListDTO {
    key: string;
    metadataIdentifier: string;
    type: IdentityProviderType;
    name: string;
    description: string;
    iconAssetKey: string | null;
    attributes: IdentityAttributeMapping[];
    isEnabled: boolean;
    isTestProvider: boolean;
}
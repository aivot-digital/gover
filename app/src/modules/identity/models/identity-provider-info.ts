import {IdentityProviderType} from '../enums/identity-provider-type';

export interface IdentityProviderInfo {
    type: IdentityProviderType;
    key: string;
    name: string;
    iconAssetKey: string;
    metadataIdentifier: string;
}
import {IdentityAttributeMapping} from './identity-attribute-mapping';
import {IdentityAdditionalParameter} from './identity-additional-parameter';

export interface IdentityProviderRequestDTO {
    metadataIdentifier: string;
    name: string;
    description: string;
    iconAssetKey?: string | null;
    authorizationEndpoint: string;
    tokenEndpoint: string;
    userinfoEndpoint?: string | null;
    endSessionEndpoint?: string | null;
    clientId: string;
    clientSecretKey?: string | null;
    attributes: IdentityAttributeMapping[];
    defaultScopes: string[];
    additionalParams: IdentityAdditionalParameter[];
    isEnabled: boolean;
    isTestProvider: boolean;
}
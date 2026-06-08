import {IdentityAdditionalParameter} from './identity-additional-parameter';
import {IdentityProviderListDTO} from './identity-provider-list-dto';

export interface IdentityProviderDetailsDTO extends IdentityProviderListDTO {
    authorizationEndpoint: string;
    tokenEndpoint: string;
    userinfoEndpoint?: string | null;
    endSessionEndpoint?: string | null;
    clientId: string;
    clientSecretKey?: string | null;
    defaultScopes: string[];
    additionalParams: IdentityAdditionalParameter[];
    pkceMethod?: 'S256' | null;
}
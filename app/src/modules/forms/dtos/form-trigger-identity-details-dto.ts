import {IdentityProviderType} from '../../identity/enums/identity-provider-type';

export interface FormTriggerIdentityDetailsDTO {
    type: IdentityProviderType;
    key: string;
    name: string;
    iconAssetKey?: string | null;
    metadataIdentifier: string;
}

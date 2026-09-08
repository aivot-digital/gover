import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import type {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import type {IdentityProviderType} from '../enums/identity-provider-type';

export type IdentityType = 'IdentityProvider' | 'Email';

export interface IdentityProviderOption {
    identityProviderKey: string;
    identityProviderName: string;
    identityProviderAssetKey: string | null;
    identityProviderType: IdentityProviderType;
    isAuthenticatedWithThis: boolean;
    additionalScopes: string[];
}

export interface IdentityCommunicationChoice {
    id: number;
    name: string;
    description: string;
}

export interface IdentityCommunicationState {
    required: boolean;
    ready: boolean;
    selectedBindingId: number | null;
    choices: IdentityCommunicationChoice[];
    customerLayout: GroupLayout | null;
    customerData: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
}

export interface IdentitySlot {
    id: string;
    title: string | null;
    description: string | null;
    isOptional: boolean;
    isRequired: boolean;
    allowsEmail: boolean;
    identityType: IdentityType | null;
    emailAddress: string | null;
    isReady: boolean;
    availableIdentityProviders: IdentityProviderOption[];
    communication: IdentityCommunicationState | null;
}

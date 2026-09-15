import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {IdentityProviderListDTO} from '../../../../modules/identity/models/identity-provider-list-dto';

export interface IdentityConfigElement extends BaseInputElement<ElementType.IdentityConfigElement> {
}

export interface IdentityConfigElementSlot {
    id: string | null;
    title: string | null;
    description: string | null;
    allowsMail: boolean | null;
    isOptional: boolean | null;
    options: IdentityConfigElementOption[] | null;
}

export type IdentityConfigElementSlotWithProviders = Omit<IdentityConfigElementSlot, 'options'> & {
    options: IdentityConfigElementOptionWithProvider[] | null;
}

export interface IdentityConfigElementOption {
    identityProviderKey: string | null;
    additionalScopes: string[] | null;
}

export type IdentityConfigElementOptionWithProvider = IdentityConfigElementOption & {
    provider: IdentityProviderListDTO;
}



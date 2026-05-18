import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyElement} from '../../any-element';

export interface IdentityInputFieldElement extends BaseInputElement<ElementType.IdentityInput> {
    options: IdentityInputFieldOption[] | null | undefined;
    allowsMail: boolean | null | undefined;
}

export interface IdentityInputFieldOption {
    identityProviderKey: string | null | undefined;
    additionalScopes: string[] | null | undefined;
    attributeMappings: IdentityInputFieldOptionAttributeMapping[] | null | undefined;
}

export interface IdentityInputFieldOptionAttributeMapping {
    fromIdentityProviderAttribute: string | null | undefined;
    toFormElementWithId: string | null | undefined;
}

export interface IdentityInputFieldElementItem {
    identityProviderKey: string | null | undefined;
    identityAttributes: Record<string, unknown> | null | undefined;
}

export function isIdentityInputFieldElement(element: AnyElement): element is IdentityInputFieldElement {
    return element.type === ElementType.IdentityInput;
}

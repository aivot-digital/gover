import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {CountryCode} from 'libphonenumber-js';

export enum PhoneNumberValidation {
    Possible = 'possible',
    Strict = 'strict',
}

export interface PhoneNumberFieldElement extends BaseInputElement<ElementType.PhoneNumber> {
    defaultCountry?: CountryCode | null;
    preferredCountries?: CountryCode[] | null;
    validationMode?: PhoneNumberValidation | null;
    autocomplete?: string | null;
    placeholder?: string | null;
}

import {type AnyElement} from '../elements/any-element';
import {type CustomerInput} from '../customer-input';

export interface FormState {
    visibilities: Record<string, boolean>;
    values: CustomerInput;
    errors: Record<string, string>;
    overrides: Record<string, AnyElement>;
}
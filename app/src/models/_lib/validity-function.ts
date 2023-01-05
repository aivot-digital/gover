import {AnyInputElement} from '../elements/form-elements/input-elements/any-input-element';

export type ValidityFunction = ($global: any, $element: AnyInputElement, $id: string) => null | string;

import {AnyInputElement} from '../elements/form/input/any-input-element';

export type ValidityFunction = ($global: any, $element: AnyInputElement, $id: string) => null | string;

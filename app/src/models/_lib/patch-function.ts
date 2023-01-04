import {AnyElement} from '../elements/any-element';

export type PatchFunction = ($global: any, $element: AnyElement, $id: string) => Partial<AnyElement>;

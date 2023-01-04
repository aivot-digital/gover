import {AnyElement} from '../../models/elements/any-element';

export interface ElementTreeProps<T extends AnyElement> {
    element: T;
    onPatch: (patch: Partial<T>) => void;
}

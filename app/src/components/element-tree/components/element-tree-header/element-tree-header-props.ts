import {AnyElement} from '../../../../models/elements/any-element';

export interface ElementTreeHeaderProps<T extends AnyElement> {
    element: T;
    onPatch: (update: Partial<T>) => void;
}

import {AnyElement} from '../../../../models/elements/any-element';


export interface ElementTreeItemProps<T extends AnyElement> {
    element: T;
    onPatch: (patch: Partial<T>) => void;
    onDelete: () => void;
    onClone: () => void;
}

import {type AnyElement} from '../../models/elements/any-element';
import {type AnyElementWithChildren} from '../../models/elements/any-element-with-children';

export interface ElementTreeItemDropTargetProps<T extends AnyElementWithChildren> {
    element: T;
    children?: any;
    isPlaceholder?: boolean;
    onDrop: (element: AnyElement) => void;
}

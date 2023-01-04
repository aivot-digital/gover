import {AnyElementWithChildren} from '../../../../models/elements/any-element-with-children';

export interface ElementTreeItemListProps<T extends AnyElementWithChildren> {
    element: T;
    isRootList?: boolean;
    onPatch: (patch: Partial<T>) => void;
}

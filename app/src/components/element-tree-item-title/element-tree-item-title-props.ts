import {type AnyElement} from '../../models/elements/any-element';

export interface ElementTreeItemTitleProps<T extends AnyElement> {
    element: T;
    isExpanded?: boolean;
    onToggleExpanded?: () => void;
    onShowAddDialog?: () => void;
    onSelect: () => void;
    editable: boolean;
}

import {type AnyElement} from '../models/elements/any-element';
import {ElementTreeScope} from './element-tree/element-tree-scope';

export interface EditorDispatcherProps<T extends AnyElement> {
    props: T;
    onPatch: (path: Partial<T>) => void;

    additionalTabIndex?: number;

    editable: boolean;

    scope: ElementTreeScope;

    hasSummaryLayoutParent: boolean;
}

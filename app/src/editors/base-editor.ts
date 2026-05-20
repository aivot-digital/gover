import {type AnyElement} from '../models/elements/any-element';
import {ElementTreeScope} from '../components/element-tree/element-tree-scope';

export interface BaseEditorProps<T extends AnyElement> {
    element: T;
    onPatch: (patch: Partial<T>) => void;
    editable: boolean;
    hasSummaryLayoutParent: boolean;
    scope: ElementTreeScope;
}

export type BaseEditor<M extends AnyElement> = (props: BaseEditorProps<M>) => React.ReactNode;

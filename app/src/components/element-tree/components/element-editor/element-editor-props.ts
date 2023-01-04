import {AnyElement} from '../../../../models/elements/any-element';

export interface ElementEditorProps<T extends AnyElement> {
    element: T;
    onSave: (update: T) => void;
    onDelete?: () => void;
    onCancel: () => void;
    onClone?: () => void;
}

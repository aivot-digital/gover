import {type AnyElement} from '../models/elements/any-element';
import {ElementTreeEntity} from './element-tree/element-tree-entity';

export interface EditorDispatcherProps<T extends AnyElement, E extends ElementTreeEntity> {
    props: T;
    onPatch: (path: Partial<T>) => void;

    entity: E;
    onPatchEntity: (path: Partial<E>) => void;

    additionalTabIndex?: number;

    editable: boolean;
}

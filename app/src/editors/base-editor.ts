import {type AnyElement} from '../models/elements/any-element';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import type {ElementTreeScope} from '../components/element-tree/element-tree-scope';

export interface BaseEditorProps<T extends AnyElement, E extends ElementTreeEntity> {
    element: T;
    onPatch: (patch: Partial<T>) => void;
    entity: E;
    onPatchEntity: (update: Partial<E>) => void;
    editable: boolean;
    scope: ElementTreeScope;
}

export type BaseEditor<M extends AnyElement, E extends ElementTreeEntity> = (props: BaseEditorProps<M, E>) => React.ReactNode;

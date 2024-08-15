import {type AnyElement} from '../../models/elements/any-element';
import type {ElementTreeEntity} from '../element-tree/element-tree-entity';

export interface ElementEditorMetadataTabProps<T extends AnyElement, E extends ElementTreeEntity> {
    elementModel: T;
    onChange: (model: T) => void;
    editable: boolean;
    entity: E;
}

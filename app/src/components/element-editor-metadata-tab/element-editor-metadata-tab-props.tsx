import {type AnyElement} from '../../models/elements/any-element';
import type {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {ElementWithParents} from '../../utils/flatten-elements';

export interface ElementEditorMetadataTabProps<T extends AnyElement, E extends ElementTreeEntity> {
    allElements: ElementWithParents[];
    elementModel: T;
    onChange: (model: T) => void;
    editable: boolean;
    entity: E;
}

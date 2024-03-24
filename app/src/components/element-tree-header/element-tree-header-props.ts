import {type RootElement} from '../../models/elements/root-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {type ElementTreeScope} from '../element-tree/element-tree-scope';

export interface ElementTreeHeaderProps<T extends RootElement | GroupLayout, E extends ElementTreeEntity> {
    entity: E;
    element: T;
    onPatch: (updatedElement: Partial<T>, updatedEntity: Partial<E>) => void;
    editable: boolean;
    scope: ElementTreeScope;
}

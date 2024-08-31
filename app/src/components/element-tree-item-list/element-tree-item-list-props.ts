import {type AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {type RootElement} from '../../models/elements/root-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {type ElementTreeScope} from '../element-tree/element-tree-scope';


export interface ElementTreeItemListProps<T extends AnyElementWithChildren, E extends ElementTreeEntity> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    entity: E;
    element: T;
    isRootList?: boolean;
    onPatch: (updatedElement: Partial<T>, updatedEntity: Partial<E>) => void;
    editable: boolean;
    scope: ElementTreeScope;
}

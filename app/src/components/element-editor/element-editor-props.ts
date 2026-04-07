import {type AnyElement} from '../../models/elements/any-element';
import {type RootElement} from '../../models/elements/root-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {type ElementTreeScope} from '../element-tree/element-tree-scope';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {SummaryLayoutElement} from '../../models/elements/form/layout/summary-layout-element';

export interface ElementEditorProps<T extends AnyElement, E extends ElementTreeEntity> {
    open: boolean;
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout | SummaryLayoutElement>;
    entity: E;
    element: T;
    onSave: (updatedElement: Partial<T>, updatedEntity: Partial<E>) => void;
    onDelete?: () => void;
    onCancel: () => void;
    onClone?: () => void;
    editable: boolean;
    scope: ElementTreeScope;
    rootEditor: boolean;
    lockMessage?: string;
}

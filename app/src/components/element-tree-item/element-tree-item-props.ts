import {type AnyElement} from '../../models/elements/any-element';
import {type RootElement} from '../../models/elements/root-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {type ElementTreeScope} from '../element-tree/element-tree-scope';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {AnyFormElement} from '../../models/elements/form/any-form-element';
import {IdentityProviderInfo} from '../../modules/identity/models/identity-provider-info';


export interface ElementTreeItemProps<T extends AnyElement, E extends ElementTreeEntity> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    entity: E;
    element: T;
    onPatch: (updatedElement: Partial<T>, updatedEntity: Partial<E>) => void;
    onMove: (updatedElement: Partial<T>, droppedElement: StepElement | AnyFormElement) => void;
    onDelete: () => void;
    onClone: () => void;
    editable: boolean;
    scope: ElementTreeScope;
    disableDrag?: boolean;
    enabledIdentityProviderInfos: IdentityProviderInfo[];
}

import {type AnyElement} from '../../../../models/elements/any-element';
import {type RootElement} from '../../../../models/elements/root-element';
import {type StepElement} from '../../../../models/elements/steps/step-element';
import {type GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {type ReplicatingContainerLayout} from '../../../../models/elements/form/layout/replicating-container-layout';
import {type Application} from '../../../../models/entities/application';
import {type Preset} from '../../../../models/entities/preset';


export interface ElementTreeItemProps<T extends AnyElement, E extends Application | Preset> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    entity: E;
    element: T;
    onPatch: (updatedElement: Partial<T>, updatedEntity: Partial<E>) => void;
    onDelete: () => void;
    onClone: () => void;
    editable: boolean;
}

import { type AnyElement } from '../../../../models/elements/any-element';
import { type RootElement } from '../../../../models/elements/root-element';
import { type StepElement } from '../../../../models/elements/steps/step-element';
import { type GroupLayout } from '../../../../models/elements/form/layout/group-layout';
import { type ReplicatingContainerLayout } from '../../../../models/elements/form/layout/replicating-container-layout';


export interface ElementTreeItemProps<T extends AnyElement> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: T;
    onPatch: (patch: Partial<T>) => void;
    onDelete: () => void;
    onClone: () => void;
    editable: boolean;
}

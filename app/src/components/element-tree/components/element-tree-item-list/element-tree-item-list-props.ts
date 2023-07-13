import { type AnyElementWithChildren } from '../../../../models/elements/any-element-with-children';
import { type RootElement } from '../../../../models/elements/root-element';
import { type StepElement } from '../../../../models/elements/steps/step-element';
import { type GroupLayout } from '../../../../models/elements/form/layout/group-layout';
import { type ReplicatingContainerLayout } from '../../../../models/elements/form/layout/replicating-container-layout';

export interface ElementTreeItemListProps<T extends AnyElementWithChildren> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: T;
    isRootList?: boolean;
    onPatch: (patch: Partial<T>) => void;
    editable: boolean;
}

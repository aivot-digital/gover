import {AnyElementWithChildren} from '../../../../models/elements/any-element-with-children';
import {RootElement} from "../../../../models/elements/root-element";
import {StepElement} from "../../../../models/elements/steps/step-element";
import {GroupLayout} from "../../../../models/elements/form/layout/group-layout";
import {ReplicatingContainerLayout} from "../../../../models/elements/form/layout/replicating-container-layout";

export interface ElementTreeItemListProps<T extends AnyElementWithChildren> {
    parents: (RootElement | StepElement | GroupLayout | ReplicatingContainerLayout)[];
    element: T;
    isRootList?: boolean;
    onPatch: (patch: Partial<T>) => void;
}

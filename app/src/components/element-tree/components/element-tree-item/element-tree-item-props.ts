import {AnyElement} from '../../../../models/elements/any-element';
import {RootElement} from "../../../../models/elements/root-element";
import {StepElement} from "../../../../models/elements/steps/step-element";
import {GroupLayout} from "../../../../models/elements/form/layout/group-layout";
import {ReplicatingContainerLayout} from "../../../../models/elements/form/layout/replicating-container-layout";


export interface ElementTreeItemProps<T extends AnyElement> {
    parents: (RootElement | StepElement | GroupLayout | ReplicatingContainerLayout)[];
    element: T;
    onPatch: (patch: Partial<T>) => void;
    onDelete: () => void;
    onClone: () => void;
}

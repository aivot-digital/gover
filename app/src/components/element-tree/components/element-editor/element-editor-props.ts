import {AnyElement} from '../../../../models/elements/any-element';
import {RootElement} from "../../../../models/elements/root-element";
import {StepElement} from "../../../../models/elements/steps/step-element";
import {GroupLayout} from "../../../../models/elements/form/layout/group-layout";
import {ReplicatingContainerLayout} from "../../../../models/elements/form/layout/replicating-container-layout";

export interface ElementEditorProps<T extends AnyElement> {
    parents: (RootElement | StepElement | GroupLayout | ReplicatingContainerLayout)[];
    element: T;
    onSave: (update: T) => void;
    onDelete?: () => void;
    onCancel: () => void;
    onClone?: () => void;
}

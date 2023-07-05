import {AnyElement} from '../../../../../../models/elements/any-element';
import {EditorTab} from "../../../../../../editors";
import {RootElement} from "../../../../../../models/elements/root-element";
import {StepElement} from "../../../../../../models/elements/steps/step-element";
import {GroupLayout} from "../../../../../../models/elements/form/layout/group-layout";
import {ReplicatingContainerLayout} from "../../../../../../models/elements/form/layout/replicating-container-layout";

export interface ElementEditorContentProps<T extends AnyElement> {
    parents: (RootElement | StepElement | GroupLayout | ReplicatingContainerLayout)[];
    element: T;
    currentTab: string;
    additionalTabs: EditorTab[];
    onChange: (update: T) => void;
}

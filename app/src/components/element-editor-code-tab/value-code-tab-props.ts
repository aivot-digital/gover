import type {RootElement} from '../../models/elements/root-element';
import type {StepElement} from '../../models/elements/steps/step-element';
import type {GroupLayout} from '../../models/elements/form/layout/group-layout';
import type {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {AnyInputElement} from '../../models/elements/form/input/any-input-element';
import {ElementWithParents} from '../../utils/flatten-elements';

export interface ValueCodeTabProps {
    allElements: ElementWithParents[];
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: AnyInputElement;
    onChange: (element: Partial<AnyInputElement>) => void;
    editable: boolean;
}
import {AnyElement} from '../../models/elements/any-element';
import type {RootElement} from '../../models/elements/root-element';
import type {StepElement} from '../../models/elements/steps/step-element';
import type {GroupLayout} from '../../models/elements/form/layout/group-layout';
import type {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {ElementWithParents} from '../../utils/flatten-elements';
import {SummaryLayoutElement} from '../../models/elements/form/layout/summary-layout-element';

export interface VisibilityCodeTabProps {
    allElements: ElementWithParents[];
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout | SummaryLayoutElement>;
    element: AnyElement;
    onChange: (element: Partial<AnyElement>) => void;
    editable: boolean;
}
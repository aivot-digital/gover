import {type AnyElement} from '../models/elements/any-element';
import {type DerivedRuntimeElementData} from '../models/element-data';
import {isRootElement} from '../models/elements/form-layout-element';
import {extractVisibleFormSteps} from './visible-form-steps';

export function resolveSummaryStepIndex(
    rootElement: AnyElement,
    derivedData: DerivedRuntimeElementData,
    stepId: string,
): number {
    if (!isRootElement(rootElement)) {
        return -1;
    }

    const visibleSteps = extractVisibleFormSteps(rootElement.children, derivedData);
    return visibleSteps.findIndex((step) => step.id === stepId);
}

import {type AnyElement} from '../models/elements/any-element';
import {type DerivedRuntimeElementData} from '../models/element-data';
import {type IntroductionStepElement} from '../models/elements/steps/introduction-step-element';
import {type StepElement, isSectionElementType} from '../models/elements/steps/step-element';
import {type SummaryStepElement} from '../models/elements/steps/summary-step-element';
import {type SubmitStepElement} from '../models/elements/steps/submit-step-element';
import {type SubmittedStepElement} from '../models/elements/steps/submitted-step-element';
import {resolveVisibility} from './element-data-utils';

export type VisibleFormStepElement =
    StepElement |
    IntroductionStepElement |
    SummaryStepElement |
    SubmitStepElement |
    SubmittedStepElement;

export function extractVisibleFormSteps(
    children: AnyElement[] | null | undefined,
    derivedData: DerivedRuntimeElementData,
): VisibleFormStepElement[] {
    const visibleChildren: VisibleFormStepElement[] = [];

    for (const child of children ?? []) {
        if (isSectionElementType(child.type) && resolveVisibility(child, derivedData)) {
            visibleChildren.push(child as VisibleFormStepElement);
        }
    }

    return visibleChildren;
}

// Keeps an index inside the currently visible step range; null means there is no step to render.
export function resolveVisibleFormStepIndex(currentStep: number, totalStepCount: number): number | null {
    if (totalStepCount <= 0) {
        return null;
    }

    return Math.min(Math.max(currentStep, 0), totalStepCount - 1);
}

// Preserves the active step by id across insertions/deletions; if it was removed, falls back to a valid index.
export function resolveVisibleFormStepIndexAfterChange(
    currentStep: number,
    visibleStepIds: string[],
    previousActiveStepId: string | null | undefined,
): number | null {
    const retainedActiveStepIndex = previousActiveStepId == null ? -1 : visibleStepIds.indexOf(previousActiveStepId);

    if (retainedActiveStepIndex !== -1) {
        return retainedActiveStepIndex;
    }

    return resolveVisibleFormStepIndex(currentStep, visibleStepIds.length);
}

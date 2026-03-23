import {ElementType} from '../../data/element-type/element-type';
import {type BaseElement} from './base-element';
import {type StepElement} from './steps/step-element';
import {type IntroductionStepElement} from './steps/introduction-step-element';
import {type SummaryStepElement} from './steps/summary-step-element';
import {type SubmitStepElement} from './steps/submit-step-element';

export interface RootElement extends BaseElement<ElementType.FormLayout> {
    headline: string | null | undefined;
    tabTitle: string | null | undefined;
    children: StepElement[] | null | undefined;

    expiring: string | null | undefined;

    privacyText: string | null | undefined;
    offlineSubmissionText: string | null | undefined;
    offlineSignatureNeeded: boolean | null | undefined;

    introductionStep: IntroductionStepElement | null | undefined;
    summaryStep: SummaryStepElement | null | undefined;
    submitStep: SubmitStepElement | null | undefined;
}

export function isRootElement(obj: any): obj is RootElement {
    return obj.type === ElementType.FormLayout;
}

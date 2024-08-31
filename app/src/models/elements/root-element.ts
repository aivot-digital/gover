import {ElementType} from '../../data/element-type/element-type';
import {type BaseElement} from './base-element';
import {type StepElement} from './steps/step-element';
import {type IntroductionStepElement} from './steps/introduction-step-element';
import {type SummaryStepElement} from './steps/summary-step-element';
import {type SubmitStepElement} from './steps/submit-step-element';

export interface RootElement extends BaseElement<ElementType.Root> {
    headline?: string;
    tabTitle?: string;
    children: StepElement[];

    expiring?: string;

    privacyText?: string;
    offlineSubmissionText?: string;
    offlineSignatureNeeded?: boolean;

    introductionStep: IntroductionStepElement;
    summaryStep: SummaryStepElement;
    submitStep: SubmitStepElement;
}

export function isRootElement(obj: any): obj is RootElement {
    return obj.type === ElementType.Root;
}

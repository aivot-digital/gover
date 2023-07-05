import {ElementType} from '../../data/element-type/element-type';
import {BaseElement} from './base-element';
import {StepElement} from './steps/step-element';
import {IntroductionStepElement} from './steps/introduction-step-element';
import {SummaryStepElement} from './steps/summary-step-element';
import {SubmitStepElement} from './steps/submit-step-element';

export interface RootElement extends BaseElement<ElementType.Root> {
    headline?: string;
    tabTitle?: string;
    theme?: string;
    children: StepElement[];

    expiring?: string;
    accessLevel?: string;

    privacyText?: string;

    introductionStep: IntroductionStepElement;
    summaryStep: SummaryStepElement;
    submitStep: SubmitStepElement;
}

export function isRootElement(obj: any): obj is RootElement {
    return obj.type === ElementType.Root;
}

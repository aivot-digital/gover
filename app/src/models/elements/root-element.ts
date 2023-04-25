import {ElementType} from '../../data/element-type/element-type';
import {BaseElement} from './base-element';
import {StepElement} from './steps/step-element';
import {IntroductionStepElement} from './steps/introduction-step-element';
import {SummaryStepElement} from './steps/summary-step-element';
import {SubmitStepElement} from './steps/submit-step-element';

export interface RootElement extends BaseElement<ElementType.Root> {
    title?: string;
    headline?: string;
    tabTitle?: string;
    theme?: string;
    children: StepElement[];

    expiring?: string;
    accessLevel?: string;

    legalSupport?: number;
    technicalSupport?: number;

    imprint?: number;
    privacy?: number;
    accessibility?: number;

    privacyText?: string;

    destination?: number;

    introductionStep: IntroductionStepElement;
    summaryStep: SummaryStepElement;
    submitStep: SubmitStepElement;
}

export function isRootElement(obj: any): obj is RootElement {
    return obj.type === ElementType.Root;
}

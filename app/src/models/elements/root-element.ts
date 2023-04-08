import {ApplicationStatus} from '../../data/application-status/application-status';
import {ElementType} from '../../data/element-type/element-type';
import {BaseElement} from './base-element';
import {StepElement} from './step-elements/step-element';
import {IntroductionStepElement} from './step-elements/introduction-step-element';
import {SummaryStepElement} from './step-elements/summary-step-element';
import {SubmitStepElement} from './step-elements/submit-step-element';

export interface RootElement extends BaseElement<ElementType.Root> {
    title?: string;
    headline?: string;
    tabTitle?: string;
    lastUpdate?: string;
    status?: ApplicationStatus;
    theme?: string;

    children: StepElement[];

    expiring?: string;
    accessLevel?: string;
    legalSupport?: number;
    technicalSupport?: number;

    imprint?: number;
    privacy?: number;
    privacyText?: string;
    accessibility?: number;

    destination?: string;

    introductionStep: IntroductionStepElement;
    summaryStep: SummaryStepElement;
    submitStep: SubmitStepElement;
}

export function isRootElement(obj: any): obj is RootElement {
    return obj.type === ElementType.Root;
}

import {StepElement} from './step-element';
import {IntroductionStepElement} from './introduction-step-element';
import {SubmitStepElement} from './submit-step-element';
import {SummaryStepElement} from './summary-step-element';
import {SubmittedStepElement} from './submitted-step-element';

export type AnyStepElement = (
    StepElement |
    IntroductionStepElement |
    SubmitStepElement |
    SubmittedStepElement |
    SummaryStepElement
);

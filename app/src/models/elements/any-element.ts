import {type RootElement} from './root-element';
import {type StepElement} from './steps/step-element';
import {type IntroductionStepElement} from './steps/introduction-step-element';
import {type SubmitStepElement} from './steps/submit-step-element';
import {type SubmittedStepElement} from './steps/submitted-step-element';
import {type SummaryStepElement} from './steps/summary-step-element';
import {type AnyFormElement} from './form/any-form-element';

export type AnyElement =
    RootElement |

    StepElement |
    IntroductionStepElement |
    SubmitStepElement |
    SubmittedStepElement |
    SummaryStepElement |

    AnyFormElement;

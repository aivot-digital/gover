import {RootElement} from "./root-element";
import {StepElement} from "./steps/step-element";
import {IntroductionStepElement} from "./steps/introduction-step-element";
import {SubmitStepElement} from "./steps/submit-step-element";
import {SubmittedStepElement} from "./steps/submitted-step-element";
import {SummaryStepElement} from "./steps/summary-step-element";
import {AnyFormElement} from "./form/any-form-element";

export type AnyElement =
    RootElement |

    StepElement |
    IntroductionStepElement |
    SubmitStepElement |
    SubmittedStepElement |
    SummaryStepElement |

    AnyFormElement;

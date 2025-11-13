import {type RootElement} from './root-element';
import {type StepElement} from './steps/step-element';
import {type IntroductionStepElement} from './steps/introduction-step-element';
import {type SubmitStepElement} from './steps/submit-step-element';
import {type SubmittedStepElement} from './steps/submitted-step-element';
import {type SummaryStepElement} from './steps/summary-step-element';
import {type AnyFormElement} from './form/any-form-element';
import {ElementType} from '../../data/element-type/element-type';
import {TextFieldElement} from './form/input/text-field-element';
import {AlertElement} from './form/content/alert-element';
import {HeadlineElement} from './form/content/headline-element';
import {RichtextElement} from './form/content/richtext-element';
import {SpacerElement} from './form/content/spacer-element';
import {TimeFieldElement} from './form/input/time-field-element';
import {ImageElement} from './form/content/image-element';
import {FileUploadElement} from './form/input/file-upload-element';
import {MultiCheckboxFieldElement} from './form/input/multi-checkbox-field-element';
import {TableFieldElement} from './form/input/table-field-element';
import {SelectFieldElement} from './form/input/select-field-element';
import {RadioFieldElement} from './form/input/radio-field-element';
import {ReplicatingContainerLayout} from './form/layout/replicating-container-layout';
import {GroupLayout} from './form/layout/group-layout';
import {CheckboxFieldElement} from './form/input/checkbox-field-element';
import {DateFieldElement} from './form/input/date-field-element';
import {NumberFieldElement} from './form/input/number-field-element';

export type AnyElement =
    RootElement |

    StepElement |
    IntroductionStepElement |
    SubmitStepElement |
    SubmittedStepElement |
    SummaryStepElement |

    AnyFormElement;

export type AnyElementType<T extends ElementType> =
    T extends ElementType.Step ? StepElement :
        T extends ElementType.Alert ? AlertElement :
            T extends ElementType.Container ? GroupLayout :
                T extends ElementType.Checkbox ? CheckboxFieldElement :
                    T extends ElementType.Date ? DateFieldElement :
                        T extends ElementType.Headline ? HeadlineElement :
                            T extends ElementType.MultiCheckbox ? MultiCheckboxFieldElement :
                                T extends ElementType.Number ? NumberFieldElement :
                                    T extends ElementType.ReplicatingContainer ? ReplicatingContainerLayout :
                                        T extends ElementType.Richtext ? RichtextElement :
                                            T extends ElementType.Radio ? RadioFieldElement :
                                                T extends ElementType.Select ? SelectFieldElement :
                                                    T extends ElementType.Spacer ? SpacerElement :
                                                        T extends ElementType.Table ? TableFieldElement :
                                                            T extends ElementType.Text ? TextFieldElement :
                                                                T extends ElementType.Time ? TimeFieldElement :
                                                                    T extends ElementType.IntroductionStep ? IntroductionStepElement :
                                                                        T extends ElementType.SubmitStep ? SubmitStepElement :
                                                                            T extends ElementType.SummaryStep ? SummaryStepElement :
                                                                                T extends ElementType.Image ? ImageElement :
                                                                                    T extends ElementType.SubmittedStep ? SubmittedStepElement :
                                                                                        T extends ElementType.FileUpload ? FileUploadElement : never;







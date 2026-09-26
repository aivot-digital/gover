import {type FormLayoutElement} from './form-layout-element';
import {type StepElement} from './steps/step-element';
import {type AnyLayoutElement} from './form/layout/any-layout-element';
import {IntroductionStepElement} from './steps/introduction-step-element';
import {ElementType} from '../../data/element-type/element-type';
import {type StepperLayoutElement} from './form/layout/stepper-layout-element';
import {type TabLayoutElement} from './form/layout/tab-layout-element';

export type AnyElementWithChildren =
    FormLayoutElement |
    StepElement |
    IntroductionStepElement |
    StepperLayoutElement |
    TabLayoutElement |
    AnyLayoutElement;

export function isAnyElementWithChildren(obj: any): obj is AnyElementWithChildren {
    return obj != null &&
        'type' in obj &&
        [
            ElementType.FormLayout,
            ElementType.Step,
            ElementType.GroupLayout,
            ElementType.ReplicatingContainer,
            ElementType.IntroductionStep,
            ElementType.StepperLayout,
            ElementType.ConfigLayout,
            ElementType.TabLayout,
            ElementType.SummaryLayout,
        ].includes(obj.type);
}

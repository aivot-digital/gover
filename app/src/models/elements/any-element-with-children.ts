import {type FormLayoutElement} from './form-layout-element';
import {type StepElement} from './steps/step-element';
import {type AnyLayoutElement} from './form/layout/any-layout-element';
import {IntroductionStepElement} from './steps/introduction-step-element';
import {ElementType} from '../../data/element-type/element-type';

export type AnyElementWithChildren =
    FormLayoutElement |
    StepElement |
    IntroductionStepElement |
    AnyLayoutElement;

export function isAnyElementWithChildren(obj: any): obj is AnyElementWithChildren {
    return obj != null &&
        'type' in obj &&
        [
            ElementType.FormLayout,
            ElementType.Step,
            ElementType.IntroductionStep,
            ElementType.GroupLayout,
            ElementType.ReplicatingContainer,
            ElementType.SummaryLayout,
            ElementType.StepperLayout,
        ].includes(obj.type);
}

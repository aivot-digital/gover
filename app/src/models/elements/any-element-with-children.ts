import {type FormLayoutElement} from './form-layout-element';
import {type StepElement} from './steps/step-element';
import {type AnyLayoutElement} from './form/layout/any-layout-element';
import {IntroductionStepElement} from './steps/introduction-step-element';

export type AnyElementWithChildren =
    FormLayoutElement |
    StepElement |
    IntroductionStepElement |
    AnyLayoutElement;

export function isAnyElementWithChildren(obj: any): obj is AnyElementWithChildren {
    return obj != null && 'children' in obj && Array.isArray(obj.children);
}

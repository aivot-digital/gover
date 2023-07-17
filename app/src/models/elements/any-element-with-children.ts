import {type RootElement} from './root-element';
import {type StepElement} from './steps/step-element';
import {type AnyLayoutElement} from './form/layout/any-layout-element';

export type AnyElementWithChildren =
    RootElement |
    StepElement |
    AnyLayoutElement;

export function isAnyElementWithChildren(obj: any): obj is AnyElementWithChildren {
    return obj != null && 'children' in obj && Array.isArray(obj.children);
}

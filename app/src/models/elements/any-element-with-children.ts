import {RootElement} from './root-element';
import {AnyLayoutElement} from './form-elements/layout-elements/any-layout-element';
import {StepElement} from './step-elements/step-element';

export type AnyElementWithChildren = (
    AnyLayoutElement |
    StepElement |
    RootElement
    );

export function isAnyElementWithChildren(obj: any): obj is AnyElementWithChildren {
    return 'children' in obj && obj.children != null;
}

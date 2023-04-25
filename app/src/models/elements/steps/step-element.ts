import {ElementType} from '../../../data/element-type/element-type';
import {BaseElement} from '../base-element';
import {AnyFormElement} from '../form/any-form-element';

export interface StepElement extends BaseElement<ElementType.Step> {
    title?: string;
    icon?: string;
    children: AnyFormElement[];
}

export function isStepElement(obj: any): obj is StepElement {
    return obj.type === ElementType.Step;
}

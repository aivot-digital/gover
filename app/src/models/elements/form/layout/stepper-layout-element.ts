import {type ElementType} from '../../../../data/element-type/element-type';
import {type BaseElement} from '../../base-element';
import {type StepElement} from '../../steps/step-element';

export interface StepperLayoutElement extends BaseElement<ElementType.StepperLayout> {
    children: StepElement[];
}

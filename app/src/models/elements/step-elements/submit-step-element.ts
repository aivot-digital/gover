import {BaseElement} from '../base-element';
import {ElementType} from '../../../data/element-type/element-type';

export interface SubmitStepElement extends BaseElement<ElementType.SubmitStep> {
    textPreSubmit?: string;
    textPostSubmit?: string;
    textProcessingTime?: string;
    documentsToReceive?: string[];
}

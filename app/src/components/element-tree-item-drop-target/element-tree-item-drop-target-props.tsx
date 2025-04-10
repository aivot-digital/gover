import {type AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {AnyFormElement} from '../../models/elements/form/any-form-element';
import {StepElement} from '../../models/elements/steps/step-element';

export interface ElementTreeItemDropTargetProps<T extends AnyElementWithChildren> {
    element: T;
    children?: any;
    isPlaceholder?: boolean;
    onDrop: (element: AnyFormElement | StepElement) => void;
}

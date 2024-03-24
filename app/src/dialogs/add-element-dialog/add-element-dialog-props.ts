import {type ElementType} from '../../data/element-type/element-type';
import {type AnyElement} from '../../models/elements/any-element';

export interface AddElementDialogProps {
    show: boolean;
    parentType: ElementType;
    onAddElement: (element: AnyElement) => void;
    onClose: () => void;
}

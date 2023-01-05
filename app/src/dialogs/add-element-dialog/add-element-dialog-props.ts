import {ElementType} from '../../data/element-type/element-type';
import {AnyElement} from '../../models/elements/any-element';

export interface AddElementDialogProps {
    parentType: ElementType;
    onAddElement: (element: AnyElement) => void;
    onClose: () => void;
}

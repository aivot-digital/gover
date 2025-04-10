import {AnyElement} from '../../models/elements/any-element';

export interface SelectElementDialogProps {
    open: boolean;
    onSelect: (element: AnyElement) => void;
    onClose: () => void;
}
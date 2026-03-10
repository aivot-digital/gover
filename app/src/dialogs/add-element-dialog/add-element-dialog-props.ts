import {type ElementType} from '../../data/element-type/element-type';
import {type AnyElement} from '../../models/elements/any-element';
import {type ReactNode} from 'react';

export interface AddElementDialogProps {
    show: boolean;
    parentType: ElementType;
    onAddElement: (element: AnyElement) => void;
    onClose: () => void;
    title?: string;
    primaryActionLabel?: string;
    primaryActionIcon?: ReactNode;
    limitElementTypes?: ElementType[];
    hidePresets?: boolean;
    hideGoverStore?: boolean;
}

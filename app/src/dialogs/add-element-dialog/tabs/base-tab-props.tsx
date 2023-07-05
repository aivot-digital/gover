import {ElementType} from "../../../data/element-type/element-type";
import {AnyElement} from "../../../models/elements/any-element";

export interface BaseTabProps {
    parentType: ElementType;
    onAddElement: (element: AnyElement) => void;
}
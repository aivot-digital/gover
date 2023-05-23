import {AnyElement} from "../../models/elements/any-element";

export interface ContainerComponentProps {
    allElements: AnyElement[];
    children: AnyElement[];
    idPrefix?: string;
}

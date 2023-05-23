import {AnyElement} from "../../models/elements/any-element";

export interface ContainerComponentProps {
    children: AnyElement[];
    idPrefix?: string;
}

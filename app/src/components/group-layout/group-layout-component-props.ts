import {type AnyElement} from '../../models/elements/any-element';

export interface GroupLayoutComponentProps {
    allElements: AnyElement[];
    children: AnyElement[];
    idPrefix?: string;
}

import {type AnyElement} from '../../models/elements/any-element';

export interface StructureTabProps<T extends AnyElement> {
    elementModel: T;
    onChange: (newElementStructure: T) => void;
    editable: boolean;
}

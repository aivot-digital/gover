import {type AnyElement} from '../../models/elements/any-element';

export interface TestTabProps<T extends AnyElement> {
    elementModel: T;
    onPatch: (data: T) => void;
    editable: boolean;
}

import {AnyElement} from '../../models/elements/any-element';

export interface BaseViewProps<M extends AnyElement, V> {
    element: M;
    setValue: (value: V | null | undefined) => void;
    error?: string;
    value?: V | null;
    idPrefix?: string;
}

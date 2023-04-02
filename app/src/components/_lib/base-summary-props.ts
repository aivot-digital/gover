import {AnyElement} from '../../models/elements/any-element';

// TODO: Value Type as Generic
export interface BaseSummaryProps<M extends AnyElement> {
    model: M;
    value: any;
    idPrefix?: string;
}

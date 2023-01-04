import {AnyElement} from '../../models/elements/any-element';

export interface BaseSummaryProps<M extends AnyElement> {
    model: M;
    value: any;
    idPrefix?: string;
}

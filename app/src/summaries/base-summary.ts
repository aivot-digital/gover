import {AnyElement} from "../models/elements/any-element";
import {FunctionComponent} from "react";
import {CustomerInput} from '../models/customer-input';

// TODO: Make private
export interface BaseSummaryProps<M extends AnyElement, V> {
    allElements: AnyElement[];
    model: M;
    value: V;
    idPrefix?: string;
    allowStepNavigation?: boolean;
    showTechnical?: boolean;
    customerInput?: CustomerInput;
    isBusy?: boolean;
}

export type BaseSummary<M extends AnyElement, V> = FunctionComponent<BaseSummaryProps<M, V>>;

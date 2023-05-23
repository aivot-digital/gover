import {AnyElement} from "../models/elements/any-element";
import {FunctionComponent} from "react";

// TODO: Make private
export interface BaseSummaryProps<M extends AnyElement, V> {
    model: M;
    value: V;
    idPrefix?: string;
}

export type BaseSummary<M extends AnyElement, V> = FunctionComponent<BaseSummaryProps<M, V>>;

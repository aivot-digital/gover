import {AnyElement} from "../models/elements/any-element";
import React, {FunctionComponent} from "react";

// TODO: Make private
export interface BaseViewProps<M extends AnyElement, V> {
    element: M;
    setValue: (value: V | null | undefined) => void;
    error?: string;
    value?: V | null;
    idPrefix?: string;
    allElements: AnyElement[];
    scrollContainerRef?: React.RefObject<HTMLDivElement>;
}

export type BaseView<M extends AnyElement, V> = FunctionComponent<BaseViewProps<M, V>>;

import {AnyElement} from "../models/elements/any-element";
import React, {FunctionComponent} from "react";

export interface BaseViewProps<M extends AnyElement, V> {
    element: M;
    setValue: (value: V | null | undefined) => void;
    error?: string;
    value?: V | null | undefined;
    idPrefix?: string;
    allElements: AnyElement[];
    scrollContainerRef?: React.RefObject<HTMLDivElement>;
    isBusy: boolean;
    isDeriving: boolean;
    valueOverride?: {
        values: Record<string, any>;
        onChange: (key: string, value: any) => void;
        onBlur?: (key: string, value: any) => void;
    };
    errorsOverride?: Record<string, string>;
}

export type BaseView<M extends AnyElement, V> = FunctionComponent<BaseViewProps<M, V>>;

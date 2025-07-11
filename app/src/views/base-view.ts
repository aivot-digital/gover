import {type AnyElement} from '../models/elements/any-element';
import React, {type FunctionComponent} from 'react';
import {type ElementData} from '../models/element-data';

export interface BaseViewProps<M extends AnyElement, V> {
    element: M;
    setValue: (value: V | null | undefined, triggeringElementIds?: string[]) => void;
    onBlur: (value: V | null | undefined, triggeringElementIds?: string[]) => void;
    errors?: string[] | null | undefined;
    value?: V | null | undefined;
    allElements: AnyElement[];
    scrollContainerRef?: React.RefObject<HTMLDivElement>;
    isBusy: boolean;
    isDeriving: boolean;
    mode: 'editor' | 'viewer';
    elementData: ElementData;
    onElementDataChange: (data: ElementData, triggeringElementIds: string[]) => void;
    onElementBlur?: (data: ElementData, triggeringElementIds: string[]) => void;
    disableVisibility?: boolean;
    derivationTriggerIdQueue: string[];
}

export type BaseView<M extends AnyElement, V> = FunctionComponent<BaseViewProps<M, V>>;

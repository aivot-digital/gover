import {type AnyElement} from '../models/elements/any-element';
import React, {type FunctionComponent} from 'react';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../models/element-data';

export interface BaseViewProps<M extends AnyElement, V> {
    rootElement: AnyElement;
    element: M;
    setValue: (value: V | null | undefined, triggeringElementIds?: string[]) => void;
    onBlur: (value: V | null | undefined, triggeringElementIds?: string[]) => void;
    errors?: string[] | null | undefined;
    value?: V | null | undefined;
    allElements: AnyElement[];
    scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
    isBusy: boolean;
    isDeriving: boolean;
    mode: 'editor' | 'viewer';
    authoredElementValues: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
    rootAuthoredElementValues: AuthoredElementValues;
    rootDerivedData: DerivedRuntimeElementData;
    onAuthoredElementValuesChange: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;
    onElementBlur?: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;
    onDerivedDataChange?: (data: DerivedRuntimeElementData) => void;
    disableVisibility?: boolean;
    derivationTriggerIdQueue: string[];
}

export type BaseView<M extends AnyElement, V> = FunctionComponent<BaseViewProps<M, V>>;

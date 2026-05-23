import type {AnyElement} from '../models/elements/any-element';
import type {FunctionComponent} from 'react';
import type {AuthoredElementValues, DerivedRuntimeElementData} from '../models/element-data';

export interface BaseViewProps<M extends AnyElement, V> {
    element: M;

    isBusy: boolean;
    isDeriving: boolean;

    value?: V | null | undefined;
    setValue: (value: V | null | undefined, triggeringElementIds?: string[]) => void;
    onBlur: (value: V | null | undefined, triggeringElementIds?: string[]) => void;

    errors?: string[] | null | undefined;
    errorDetails?: Record<string, any> | null | undefined;

    authoredElementValues: AuthoredElementValues;
    onAuthoredElementValuesChange: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;
    onElementBlur?: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;

    derivedData: DerivedRuntimeElementData;
    onDerive: (data: AuthoredElementValues, triggeringElementIds: string[], skipErrorsForElements?: string[]) => Promise<DerivedRuntimeElementData>;
    onEvent: (data: AuthoredElementValues, event: string) => Promise<void>;
    onResetErrors: () => void;
    suppressErrors: boolean;

    derivationTriggerIdQueue: string[];
}

export type BaseView<M extends AnyElement, V> = FunctionComponent<BaseViewProps<M, V>>;

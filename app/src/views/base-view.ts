import type {AnyElement} from '../models/elements/any-element';
import type {FunctionComponent, RefCallback} from 'react';
import type {AuthoredElementValues, DerivedRuntimeElementData} from '../models/element-data';
import {type InputModeLiteralRenderContext} from '../components/input-mode-field/input-mode-field';
import type {
    DynamicTextInputMethods,
    DynamicTextVariableMetadata,
} from '../components/dynamic-text/dynamic-text-metadata';

export interface DynamicTextLiteralRenderContext {
    inputRef: RefCallback<DynamicTextInputMethods>;
    variableMetadata: readonly DynamicTextVariableMetadata[];
}

export interface BaseViewProps<M extends AnyElement, V> {
    element: M;

    isBusy: boolean;
    isDeriving: boolean;

    value?: V | null | undefined;
    setValue: (value: V | null, triggeringElementIds?: string[]) => void;
    onBlur: (value: V | null, triggeringElementIds?: string[]) => void;

    errors?: string[] | null | undefined;
    errorDetails?: Record<string, any> | null | undefined;

    authoredElementValues: AuthoredElementValues;
    onAuthoredElementValuesChange: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;
    onElementBlur?: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;

    derivedData: DerivedRuntimeElementData;
    onDerive: (data: AuthoredElementValues, triggeringElementIds: string[], skipErrorsForElements?: string[]) => Promise<DerivedRuntimeElementData>;
    onEvent: (data: AuthoredElementValues, event: string) => Promise<boolean | void>;
    onResetErrors: () => void;
    suppressErrors: boolean;

    derivationTriggerIdQueue: string[];

    inputModeLiteralContext?: Omit<InputModeLiteralRenderContext<V>, 'value' | 'onChange'> & {
        dynamicText?: DynamicTextLiteralRenderContext;
    };
}

export type BaseView<M extends AnyElement, V> = FunctionComponent<BaseViewProps<M, V>>;

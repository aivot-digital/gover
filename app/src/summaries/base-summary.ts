import {AnyElement} from '../models/elements/any-element';
import {FunctionComponent} from 'react';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../models/element-data';

// TODO: Make private
export interface BaseSummaryProps<M extends AnyElement, V> {
    model: M;
    value: V;
    allowStepNavigation?: boolean;
    showTechnical?: boolean;
    authoredElementValues: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
}

export type BaseSummary<M extends AnyElement, V> = FunctionComponent<BaseSummaryProps<M, V>>;

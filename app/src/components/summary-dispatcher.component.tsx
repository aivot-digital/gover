import React, {ComponentType} from 'react';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {isComponentVisible} from '../utils/is-component-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {SummaryMap} from './summary.map';
import {BaseSummaryProps} from './_lib/base-summary-props';
import {selectCustomerInput} from '../slices/customer-input-slice';
import {AnyElement} from '../models/elements/any-element';

interface DispatcherComponentProps<M extends AnyElement> {
    model: M;
    idPrefix?: string;
}

export function SummaryDispatcherComponent<M extends AnyElement>({model, idPrefix}: DispatcherComponentProps<M>) {
    const id = idPrefix != null ? (idPrefix + model.id) : model.id;

    const customerInput = useSelector(selectCustomerInput);

    const patchedModel = {
        ...model,
        ...generateComponentPatch(id, model, customerInput),
        id,
    };

    const value = (patchedModel as any).value ?? customerInput[id];

    const adminSettings = useSelector((state: RootState) => state.adminSettings);

    const isVisible = adminSettings.disableVisibility || isComponentVisible(id, model, customerInput);

    if (!isVisible) {
        return null;
    }

    const Component: ComponentType<BaseSummaryProps<M>> = SummaryMap[model.type];
    if (Component == null) {
        return null;
    }

    const viewProps: BaseSummaryProps<M> = {
        model: patchedModel,
        value,
        idPrefix,
    };

    return (
        <div id={model.id}>
            <Component {...viewProps} />
        </div>
    );
}

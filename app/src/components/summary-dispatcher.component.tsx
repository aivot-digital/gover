import React, {ComponentType} from 'react';
import {useSelector} from 'react-redux';
import {isElementVisible} from '../utils/is-element-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {SummaryMap} from './summary.map';
import {BaseSummaryProps} from './_lib/base-summary-props';
import {selectCustomerInput} from '../slices/customer-input-slice';
import {AnyElement} from '../models/elements/any-element';
import {selectDisableVisibility} from "../slices/admin-settings-slice";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";
import {evaluateFunction} from "../utils/evaluate-function";
import {CustomerInput} from "../models/customer-input";

interface DispatcherComponentProps<M extends AnyElement> {
    model: M;
    idPrefix?: string;
}

function makeValue(model: AnyElement, id: string, global?: CustomerInput): any | null | undefined {
    if (isAnyInputElement(model) && model.computeValue != null) {
        return evaluateFunction(model.computeValue, (global ?? {}), model, id, false);
    }
    return (model as any).value ?? (global ?? {})[id];
}

export function SummaryDispatcherComponent<M extends AnyElement>({model, idPrefix}: DispatcherComponentProps<M>) {
    const id = idPrefix != null ? (idPrefix + model.id) : model.id;

    const customerInput = useSelector(selectCustomerInput);
    const disableVisibility = useSelector(selectDisableVisibility)

    const patchedModel = {
        ...model,
        ...generateComponentPatch(id, model, customerInput),
        id,
    };

    const value = makeValue(patchedModel, id, customerInput);

    const isVisible = disableVisibility || isElementVisible(id, model, customerInput);

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

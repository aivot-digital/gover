import React, {ComponentType} from 'react';
import {useSelector} from 'react-redux';
import {isElementVisible} from '../utils/is-element-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {AnyElement} from '../models/elements/any-element';
import {selectDisableVisibility} from '../slices/admin-settings-slice';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {evaluateFunction} from '../utils/evaluate-function';
import {CustomerInput} from '../models/customer-input';
import Summaries from '../summaries';
import {BaseSummaryProps} from '../summaries/base-summary';
import {resolveId} from '../utils/id-utils';
import {useAppSelector} from '../hooks/use-app-selector';

interface DispatcherComponentProps<M extends AnyElement> {
    allElements: AnyElement[];
    element: M;
    idPrefix?: string;
    allowStepNavigation?: boolean;
    showTechnical?: boolean;
    customerInput?: CustomerInput;
}

function makeValue(idPrefix: string | undefined, allElements: AnyElement[], model: AnyElement, id: string, global?: CustomerInput): any | null | undefined {
    const computedValue = isAnyInputElement(model) ? evaluateFunction(idPrefix, allElements, model.computeValue, (global ?? {}), model, id, false) : undefined;
    return computedValue ?? (global ?? {})[resolveId(id, idPrefix)];
}

export function SummaryDispatcherComponent<M extends AnyElement>(props: DispatcherComponentProps<M>) {
    if (!Boolean(props.showTechnical) && isAnyInputElement(props.element) && Boolean(props.element.technical)) {
        return null;
    }

    const loadedCustomerInput = props.customerInput != null ? props.customerInput : useAppSelector(state => state.app.inputs);
    const disableVisibility = useSelector(selectDisableVisibility);

    const patchedModel = {
        ...props.element,
        ...generateComponentPatch(props.idPrefix, props.allElements, props.element.id, props.element, loadedCustomerInput),
    };

    const isVisible = disableVisibility || isElementVisible(props.idPrefix, props.allElements, props.element.id, props.element, loadedCustomerInput);

    if (!isVisible) {
        return null;
    }

    const Component: ComponentType<BaseSummaryProps<M, any>> | null = Summaries[props.element.type];
    if (Component == null) {
        return null;
    }

    const value = makeValue(props.idPrefix, props.allElements, patchedModel, props.element.id, loadedCustomerInput);

    const viewProps: BaseSummaryProps<M, any> = {
        allElements: props.allElements,
        model: patchedModel,
        value: value,
        idPrefix: props.idPrefix,
        allowStepNavigation: props.allowStepNavigation,
        showTechnical: props.showTechnical,
        customerInput: props.customerInput,
    };

    return (
        <div id={props.element.id}>
            <Component {...viewProps} />
        </div>
    );
}

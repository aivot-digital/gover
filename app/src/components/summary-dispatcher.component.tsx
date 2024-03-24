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
    if (isAnyInputElement(model) && model.computeValue != null) {
        return evaluateFunction(idPrefix, allElements, model.computeValue, (global ?? {}), model, id, false);
    }
    return (model as any).value ?? (global ?? {})[resolveId(id, idPrefix)];
}

export function SummaryDispatcherComponent<M extends AnyElement>({
                                                                     allElements,
                                                                     element,
                                                                     idPrefix,
                                                                     allowStepNavigation,
                                                                     showTechnical,
                                                                     customerInput,
                                                                 }: DispatcherComponentProps<M>) {
    if (!Boolean(showTechnical) && isAnyInputElement(element) && Boolean(element.technical)) {
        return null;
    }

    const prefixedId = idPrefix != null ? (idPrefix + element.id) : element.id;

    const loadedCustomerInput = customerInput != null ? customerInput : useAppSelector(state => state.app.inputs);
    const disableVisibility = useSelector(selectDisableVisibility);

    const patchedModel = {
        ...element,
        ...generateComponentPatch(idPrefix, allElements, element.id, element, loadedCustomerInput),
    };

    const value = makeValue(idPrefix, allElements, patchedModel, element.id, loadedCustomerInput);

    const isVisible = disableVisibility || isElementVisible(idPrefix, allElements, element.id, element, loadedCustomerInput);

    if (!isVisible) {
        return null;
    }

    const Component: ComponentType<BaseSummaryProps<M, any>> | null = Summaries[element.type];
    if (Component == null) {
        return null;
    }

    const viewProps: BaseSummaryProps<M, any> = {
        allElements: allElements,
        model: patchedModel,
        value,
        idPrefix,
        allowStepNavigation,
        showTechnical,
    };

    return (
        <div id={element.id}>
            <Component {...viewProps} />
        </div>
    );
}

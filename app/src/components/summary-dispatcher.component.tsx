import React, {ComponentType} from 'react';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {CustomerInput} from '../models/customer-input';
import Summaries from '../summaries';
import {BaseSummaryProps} from '../summaries/base-summary';
import {resolveId} from '../utils/id-utils';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectOverride, selectValue, selectVisibility} from '../slices/app-slice';
import {FunctionType, hasElementFunctionType} from '../utils/function-status-utils';

interface DispatcherComponentProps<M extends AnyElement> {
    allElements: AnyElement[];
    element: M;
    idPrefix?: string;
    allowStepNavigation?: boolean;
    showTechnical?: boolean;
    customerInput?: CustomerInput;
    isBusy?: boolean;
}

export function SummaryDispatcherComponent<M extends AnyElement>(props: DispatcherComponentProps<M>) {
    if (!Boolean(props.showTechnical) && isAnyInputElement(props.element) && Boolean(props.element.technical)) {
        return null;
    }

    const id = resolveId(props.element.id, props.idPrefix);

    const isVisible = useAppSelector(selectVisibility(id));
    const value = useAppSelector(selectValue(id));
    const override = useAppSelector(selectOverride(id));

    const patchedModel = {
        ...props.element,
        ...override,
    };

    if (!isVisible) {
        return null;
    }

    const Component: ComponentType<BaseSummaryProps<M, any>> | null = Summaries[props.element.type];
    if (Component == null) {
        return null;
    }

    const viewProps: BaseSummaryProps<M, any> = {
        allElements: props.allElements,
        model: patchedModel,
        value: value,
        idPrefix: props.idPrefix,
        allowStepNavigation: props.allowStepNavigation,
        showTechnical: props.showTechnical,
        customerInput: props.customerInput,
        isBusy: props.isBusy ?? false,
    };

    return (
        <div id={props.element.id}>
            <Component {...viewProps} />
        </div>
    );
}

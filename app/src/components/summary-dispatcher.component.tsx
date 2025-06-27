import React, {useMemo} from 'react';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {CustomerInput} from '../models/customer-input';
import Summaries from '../summaries';
import {BaseSummaryProps} from '../summaries/base-summary';
import {resolveId} from '../utils/id-utils';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectComputedValue, selectCustomerInputValue, selectOverride, selectVisibility} from '../slices/app-slice';

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
    const {
        allElements,
        element: initialElement,
        idPrefix,
        showTechnical,
        allowStepNavigation,
        customerInput,
        isBusy,
    } = props;

    const {
        id: initialElementId,
    } = initialElement;

    const resolvedId = useMemo(() => resolveId(initialElementId, idPrefix), [initialElementId, idPrefix]);

    const isVisibleComputed = useAppSelector(selectVisibility(resolvedId));
    const customerInputValue = useAppSelector(selectCustomerInputValue(resolvedId));
    const computedValue = useAppSelector(selectComputedValue(resolvedId));
    const override = useAppSelector(selectOverride(resolvedId));

    const element: M = useMemo(() => ({
        ...(override ?? initialElement),
    } as M), [initialElement, override]);

    const Component = useMemo(() => {
        return Summaries[element.type];
    }, [element.type]);

    const isVisible = useMemo(() => {
        if (!isVisibleComputed) {
            return false;
        }

        if (isAnyInputElement(element) && element.technical && showTechnical !== true) {
            return false;
        }

        if (Component == null) {
            console.warn(`No summary component found for element type: ${element.type}`);
            return false;
        }

        return true;
    }, [isVisibleComputed, element, showTechnical, Component]);

    const value = useMemo(() => {
        if (isAnyInputElement(element) && (element.disabled || element.technical)) {
            return computedValue;
        }

        return customerInputValue ?? computedValue;
    }, [element, customerInputValue, computedValue]);

    const viewProps: BaseSummaryProps<M, any> = useMemo(() => ({
        allElements: allElements,
        model: element,
        value: value,
        idPrefix: idPrefix,
        allowStepNavigation: allowStepNavigation,
        showTechnical: showTechnical,
        customerInput: customerInput,
        isBusy: isBusy ?? false,
    }), [allElements, element, value, idPrefix, allowStepNavigation, showTechnical, customerInput, isBusy]);

    if (Component == null || !isVisible) {
        return null;
    }

    return (
        <div id={props.element.id}>
            <Component {...viewProps} />
        </div>
    );
}

import React, {ComponentType} from 'react';
import {isElementVisible} from '../utils/is-element-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {selectCustomerInput, updateUserInput} from '../slices/customer-input-slice';
import {Grid} from '@mui/material';
import {AnyElement} from '../models/elements/any-element';
import {evaluateFunction} from "../utils/evaluate-function";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";
import Views from "../views";
import {BaseViewProps} from "../views/base-view";
import {useAppSelector} from "../hooks/use-app-selector";
import {selectDisableVisibility} from "../slices/admin-settings-slice";
import {ErrorBoundary} from "./error-boundary/error-boundary";
import {selectCustomerInputErrorValue} from "../slices/customer-input-errors-slice";
import {useAppDispatch} from "../hooks/use-app-dispatch";

interface DispatcherComponentProps<M extends AnyElement> {
    allElements: AnyElement[];
    element: M;
    idPrefix?: string;
}

function makeId(element: AnyElement, idPrefix?: string | null): string {
    return idPrefix != null ? (idPrefix + element.id) : element.id;
}


export function ViewDispatcherComponent<M extends AnyElement, V>({
                                                                     allElements,
                                                                     element,
                                                                     idPrefix
                                                                 }: DispatcherComponentProps<M>) {
    const dispatch = useAppDispatch();

    const id = makeId(element, idPrefix);

    const userInputData = useAppSelector(selectCustomerInput);
    const disableVisibility = useAppSelector(selectDisableVisibility);
    const error = useAppSelector(selectCustomerInputErrorValue(id));

    const isVisible = disableVisibility || isElementVisible(allElements, id, element, userInputData);

    if (!isVisible) {
        return null;
    }

    const Component: ComponentType<BaseViewProps<M, V>> | null = Views[element.type];
    if (Component == null) {
        return null;
    }

    const patchedModel = {
        ...element,
        ...generateComponentPatch(allElements, id, element, userInputData),
        id,
    };

    const value = isAnyInputElement(element) ? (evaluateFunction(allElements, element.computeValue, userInputData, element, id, false) ?? userInputData[id]) : null;

    const viewProps: BaseViewProps<M, V> = {
        allElements: allElements,
        element: patchedModel,
        setValue: (value: V | null | undefined) => dispatch(updateUserInput({
            key: id,
            value,
        })),
        error: error,
        value,
        idPrefix,
    };

    return (
        <Grid
            item
            xs={('weight' in element && element.weight != null) ? element.weight : 12}
            id={element.id}
        >
            <ErrorBoundary>
                <Component {...viewProps} />
            </ErrorBoundary>
        </Grid>
    );
}

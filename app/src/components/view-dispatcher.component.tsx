import React, {ComponentType} from 'react';
import {isElementVisible} from '../utils/is-element-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {Grid} from '@mui/material';
import {AnyElement} from '../models/elements/any-element';
import {evaluateFunction} from "../utils/evaluate-function";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";
import Views from "../views";
import {BaseViewProps} from "../views/base-view";
import {useAppSelector} from "../hooks/use-app-selector";
import {selectDisableVisibility} from "../slices/admin-settings-slice";
import {ErrorBoundary} from "./error-boundary/error-boundary";
import {useAppDispatch} from "../hooks/use-app-dispatch";
import {makeId} from "../utils/id-utils";
import {selectCustomerInputError, selectDisabled, setDisabled, updateCustomerInput} from '../slices/app-slice';

interface DispatcherComponentProps<M extends AnyElement, V> {
    allElements: AnyElement[];
    element: M;
    idPrefix?: string;
    scrollContainerRef?: React.RefObject<HTMLDivElement>;
}


export function ViewDispatcherComponent<M extends AnyElement, V>(props: DispatcherComponentProps<M, V>) {
    const dispatch = useAppDispatch();

    const id = makeId(props.element, props.idPrefix);

    const customerInputs = useAppSelector(state => state.app.inputs);
    const disableVisibility = useAppSelector(selectDisableVisibility);
    const error = useAppSelector(selectCustomerInputError(id));

    const isVisible = disableVisibility || isElementVisible(props.idPrefix, props.allElements, props.element.id, props.element, customerInputs);
    const isDisabled = useAppSelector(selectDisabled(id));

    if (!isVisible) {
        return null;
    }

    const Component: ComponentType<BaseViewProps<M, V>> | null = Views[props.element.type];
    if (Component == null) {
        return null;
    }

    const patchedModel = {
        ...props.element,
        ...generateComponentPatch(props.idPrefix, props.allElements, props.element.id, props.element, customerInputs),
        id,
    };

    if (isDisabled) {
        patchedModel.disabled = true;
    }

    const computedValue = isAnyInputElement(props.element) ? evaluateFunction(props.idPrefix, props.allElements, props.element.computeValue, customerInputs, props.element, props.element.id, false) : undefined;
    const storedValue = customerInputs[id];
    if (computedValue != null && computedValue !== storedValue) {
        dispatch(updateCustomerInput({
            key: id,
            value: computedValue,
        }));
        dispatch(setDisabled({
            key: id,
            value: true,
        }));
    }
    const value = computedValue ?? customerInputs[id];

    if (isAnyInputElement(props.element) && Boolean(props.element.technical)) {
        return null;
    }

    const viewProps: BaseViewProps<M, V> = {
        allElements: props.allElements,
        element: patchedModel,
        setValue: (value: V | null | undefined) => {
            dispatch(updateCustomerInput({
                key: id,
                value: value,
            }));
        },
        error: error,
        value: value,
        idPrefix: props.idPrefix,
        scrollContainerRef: props.scrollContainerRef,
    };

    return (
        <Grid
            item
            xs={12}
            md={('weight' in props.element && props.element.weight != null) ? props.element.weight : 12}
            id={id}
            sx={{position: 'relative'}}
        >
            <ErrorBoundary>
                <Component {...viewProps} />
            </ErrorBoundary>
        </Grid>
    );
}

import React, {ComponentType, useCallback, useMemo} from 'react';
import Grid from '@mui/material/Grid';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {views as Views} from '../views';
import {BaseViewProps} from '../views/base-view';
import {useAppSelector} from '../hooks/use-app-selector';
import {ElementErrorBoundary} from './element-error-boundary/element-error-boundary';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {resolveId} from '../utils/id-utils';
import {enqueueDerivationTriggerId, selectComputedValue, selectCustomerInputValue, selectDisabled, selectError, selectFunctionReferences, selectOverride, selectVisibility, updateCustomerInput} from '../slices/app-slice';
import {FunctionType, hasElementFunctionType} from '../utils/function-status-utils';
import {selectDisableVisibility} from '../slices/admin-settings-slice';

interface DispatcherComponentProps<M extends AnyElement, V> {
    allElements: AnyElement[];
    element: M;
    idPrefix?: string;
    scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
    isBusy: boolean;
    isDeriving: boolean;
    mode: 'editor' | 'viewer';

    valueOverride?: {
        values: Record<string, any>;
        onChange: (key: string, value: any) => void;
        onBlur?: (key: string, value: any) => void;
    };
    errorsOverride?: Record<string, string>;
    visibilitiesOverride?: Record<string, boolean>;
    overridesOverride?: Record<string, AnyElement>;
}

export function ViewDispatcherComponent<M extends AnyElement, V>(props: DispatcherComponentProps<M, V>) {
    const dispatch = useAppDispatch();

    const {
        element: initialElement,
        idPrefix,
        valueOverride,
        errorsOverride,
        allElements,
        scrollContainerRef,
        isBusy,
        isDeriving,
        mode,
        visibilitiesOverride,
        overridesOverride,
    } = props;

    const {
        id: initialElementId,
    } = initialElement;

    const resolvedId = useMemo(() => {
        return resolveId(initialElementId, idPrefix);
    }, [initialElementId, idPrefix]);

    const storedIsVisible = useAppSelector(selectVisibility(resolvedId));
    const isDisabled = useAppSelector(selectDisabled(resolvedId));
    const customerInputValue = useAppSelector(selectCustomerInputValue(resolvedId));
    const computedValue = useAppSelector(selectComputedValue(resolvedId));
    const override = useAppSelector(selectOverride(resolvedId));
    const storedError = useAppSelector(selectError(resolvedId));
    const references = useAppSelector(selectFunctionReferences);
    const disableVisibility = useAppSelector(selectDisableVisibility);

    const hasReferences = useMemo(() => {
        if (references == null) {
            return false;
        }
        return references
            .some(ref => (
                ref.target.id === initialElementId &&
                (ref.isSameStep || ref.sourceIsStep) &&
                ref.functionType !== FunctionType.VALIDATION
            ));
    }, [initialElementId, references]);

    const element: AnyElement = useMemo(() => {
        const element = {
            ...((overridesOverride != null ? overridesOverride[resolvedId] : override) ?? initialElement),
            id: resolvedId,
        };

        if (isAnyInputElement(element)) {
            element.disabled = element.disabled || isDisabled;
        }

        return element;
    }, [overridesOverride, override, resolvedId, isDisabled, initialElement]);

    const value = useMemo(() => {
        if (valueOverride != null) {
            return valueOverride.values[resolvedId];
        }

        if (isAnyInputElement(element) && (element.disabled || element.technical)) {
            return computedValue;
        }

        return customerInputValue ?? computedValue;
    }, [element, customerInputValue, valueOverride, resolvedId, computedValue]);

    const error: string | undefined | null = useMemo(() => {
        if (errorsOverride != null) {
            return errorsOverride[resolvedId];
        }
        return storedError;
    }, [errorsOverride, storedError, resolvedId]);

    const handleSetValue = useCallback((updatedValue: V | null | undefined) => {
        if (updatedValue == value) {
            return;
        }
        if (valueOverride != null) {
            valueOverride.onChange(resolvedId, updatedValue);
        } else {
            // Keep this order of operations to guarantee that listeners consuming the derivation queue have the updated customer input in place
            dispatch(updateCustomerInput({
                key: resolvedId,
                value: updatedValue,
            }));
            // Check if the element has references to trigger the derivation queue
            if (hasReferences || (updatedValue == null && hasElementFunctionType(initialElement, FunctionType.VALUE))) {
                dispatch(enqueueDerivationTriggerId(initialElementId));
            }
        }
    }, [value, valueOverride, resolvedId, hasReferences, initialElement, initialElementId, dispatch]);

    const Component: ComponentType<BaseViewProps<typeof element, V>> | null = useMemo(() => Views[element.type], [element.type]);

    const isVisible = useMemo(() => {
        if (isAnyInputElement(element) && element.technical && (mode !== 'editor' || !disableVisibility)) {
            return false;
        }

        if (visibilitiesOverride != null) {
            return visibilitiesOverride[resolvedId] ?? true;
        } else {
            return storedIsVisible;
        }
    }, [storedIsVisible, element, mode, disableVisibility, visibilitiesOverride]);

    const viewProps: BaseViewProps<typeof element, V> = useMemo(() => ({
        allElements: allElements,
        element: element,
        setValue: handleSetValue,
        error: error,
        value: value,
        idPrefix: idPrefix,
        scrollContainerRef: scrollContainerRef,
        isBusy: isBusy,
        isDeriving: isDeriving,
        valueOverride: valueOverride,
        errorsOverride: errorsOverride,
        visibilitiesOverride: visibilitiesOverride,
        overridesOverride: overridesOverride,
        mode: mode,
    }), [allElements, element, error, value, handleSetValue, idPrefix, scrollContainerRef, isDeriving, mode, isBusy, visibilitiesOverride, overridesOverride]);

    if (Component == null || !isVisible) {
        return null;
    }

    return (
        <Grid
            item
            xs={12}
            md={('weight' in element && element.weight != null) ? element.weight : 12}
            id={resolvedId}
            data-initial-id={initialElementId}
            data-resolved-id={resolvedId}
            sx={{
                position: 'relative',
            }}
        >
            <ElementErrorBoundary viewProps={viewProps}>
                <Component {...viewProps} />
            </ElementErrorBoundary>
        </Grid>
    );
}

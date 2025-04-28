import React, {ComponentType, useMemo} from 'react';
import {Grid} from '@mui/material';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import Views from '../views';
import {BaseViewProps} from '../views/base-view';
import {useAppSelector} from '../hooks/use-app-selector';
import {ElementErrorBoundary} from './element-error-boundary/element-error-boundary';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {resolveId} from '../utils/id-utils';
import {enqueueDerivationTriggerId, selectDisabled, selectError, selectFunctionReferences, selectOverride, selectValue, selectVisibility, updateCustomerInput} from '../slices/app-slice';
import {FunctionType} from '../utils/function-status-utils';

interface DispatcherComponentProps<M extends AnyElement, V> {
    allElements: AnyElement[];
    element: M;
    idPrefix?: string;
    scrollContainerRef?: React.RefObject<HTMLDivElement>;
    isBusy: boolean;
    isDeriving: boolean;
    mode: 'editor' | 'viewer';

    valueOverride?: {
        values: Record<string, any>;
        onChange: (key: string, value: any) => void;
        onBlur?: (key: string, value: any) => void;
    };
    errorsOverride?: Record<string, string>;
}

export function ViewDispatcherComponent<M extends AnyElement, V>(props: DispatcherComponentProps<M, V>) {
    const dispatch = useAppDispatch();

    const resolvedId = useMemo(() => {
        return resolveId(props.element.id, props.idPrefix);
    }, [props.element, props.idPrefix]);

    const isVisible = useAppSelector(selectVisibility(resolvedId));
    const isDisabled = useAppSelector(selectDisabled(resolvedId));
    const customerInputValue = useAppSelector(selectValue(resolvedId));
    const override = useAppSelector(selectOverride(resolvedId));
    const error = useAppSelector(selectError(resolvedId));
    const references = useAppSelector(selectFunctionReferences);

    const hasReferences = useMemo(() => {
        if (references == null) {
            return false;
        }
        return references
            .some(ref => (
                ref.target.id === props.element.id &&
                (ref.isSameStep || ref.sourceIsStep) &&
                ref.functionType !== FunctionType.VALIDATION
            ));
    }, [props.element]);

    const value = useMemo(() => {
        if (props.valueOverride != null) {
            return props.valueOverride.values[resolvedId];
        }
        return customerInputValue;
    }, [customerInputValue, props.valueOverride, resolvedId]);

    const element: AnyElement = useMemo(() => {
        const element = {
            ...(override ?? props.element),
            id: resolvedId,
        };

        if (isAnyInputElement(element)) {
            element.disabled = element.disabled || isDisabled;
        }

        return element;
    }, [override, resolvedId, isDisabled, props.element]);

    const Component: ComponentType<BaseViewProps<typeof element, V>> | null = useMemo(() => Views[element.type], [element.type]);
    if (Component == null) {
        return null;
    }

    const viewProps: BaseViewProps<typeof element, V> = useMemo(() => ({
        allElements: props.allElements,
        element: element,
        setValue: (updatedValue: V | null | undefined) => {
            if (updatedValue == value) {
                return;
            }

            if (props.valueOverride != null) {
                props.valueOverride.onChange(resolvedId, updatedValue);
            } else {
                // Keep this order of operations to guarantee that listeners consuming the derivation queue have the updated customer input in place
                dispatch(updateCustomerInput({
                    key: resolvedId,
                    value: updatedValue,
                }));
                // Check if the element has references to trigger the derivation queue
                if (hasReferences) {
                    dispatch(enqueueDerivationTriggerId(props.element.id));
                }
            }
        },
        error: props.errorsOverride != null ? props.errorsOverride[resolvedId] : error,
        value: props.valueOverride != null ? props.valueOverride.values[resolvedId] : value,
        idPrefix: props.idPrefix,
        scrollContainerRef: props.scrollContainerRef,
        isBusy: props.isBusy,
        isDeriving: props.isDeriving,
        valueOverride: props.valueOverride,
        errorsOverride: props.errorsOverride,
        mode: props.mode,
    }), [element, resolvedId, error, value, dispatch, props]);

    if (!isVisible || (props.mode !== 'editor' && isAnyInputElement(element) && element.technical)) {
        return null;
    }

    return (
        <Grid
            item
            xs={12}
            md={('weight' in element && element.weight != null) ? element.weight : 12}
            id={resolvedId}
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

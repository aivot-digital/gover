import React, {ComponentType, useCallback, useMemo} from 'react';
import Grid from '@mui/material/Grid';
import {type AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {views as Views} from '../views';
import {type BaseViewProps} from '../views/base-view';
import {ElementErrorBoundary} from './element-error-boundary/element-error-boundary';
import {type AnyElementDataObject, type ElementData} from '../models/element-data';

interface DispatcherComponentProps<T extends AnyElement> {
    allElements: AnyElement[];
    element: T;

    scrollContainerRef?: React.RefObject<HTMLDivElement>;
    isBusy: boolean;
    isDeriving: boolean;
    mode: 'editor' | 'viewer';

    elementData: ElementData;
    onElementDataChange: (data: ElementData, triggeringElementIds: string[]) => void;
    onElementBlur?: (data: ElementData, triggeringElementIds: string[]) => void;

    disableVisibility?: boolean;
    derivationTriggerIdQueue: string[];
}

export function ViewDispatcherComponent<T extends AnyElement>(props: DispatcherComponentProps<T>) {
    const {
        element: initialElement,
        allElements,
        scrollContainerRef,
        isBusy,
        isDeriving,
        mode,
        elementData,
        onElementDataChange,
        onElementBlur,
        disableVisibility,
        derivationTriggerIdQueue,
    } = props;

    const {
        id: elementId,
        type: elementType,
    } = initialElement;

    const elementDataObject: AnyElementDataObject = useMemo(() => {
        return elementData[elementId] ?? {
            $type: elementType,
            inputValue: undefined,
            isVisible: true,
            isPrefilled: false,
            isDirty: false,
            computedOverride: undefined,
            computedValue: undefined,
            computedErrors: undefined,
            value: undefined,
        };
    }, [elementData, elementId, elementType]);

    const element: AnyElement = useMemo(() => {
        return elementDataObject.computedOverride ?? initialElement;
    }, [elementDataObject, initialElement]);

    const value = useMemo(() => {
        const inputValue = elementDataObject.inputValue;
        const computedValue = elementDataObject.computedValue;

        if (isAnyInputElement(element) && (element.disabled || element.technical)) {
            console.warn(`Returning computed value for disabled or technical element: ${element.id}`);
            return computedValue;
        }

        return inputValue ?? computedValue;
    }, [elementDataObject, element]);

    const error: string[] | undefined | null = useMemo(() => {
        return elementDataObject.computedErrors;
    }, [elementDataObject]);

    const handleSetValue = useCallback((updatedValue: any | null | undefined, triggeringElementIds?: string[]) => {
        if (updatedValue == value) {
            return;
        }

        const newElementData = {
            ...elementData,
            [elementId]: {
                ...elementDataObject,
                $type: elementType,
                inputValue: updatedValue,
                isDirty: true,
            },
        };

        onElementDataChange(newElementData, [elementId, ...(triggeringElementIds ?? [])]);
    }, [value, elementData, onElementDataChange, elementId, elementType]);

    const handleOnBlur = useCallback((updatedValue: any | null | undefined, triggeringElementIds?: string[]) => {
        if (updatedValue == value || onElementBlur == null) {
            return;
        }

        const newElementData = {
            ...elementData,
            [elementId]: {
                ...elementDataObject,
                $type: elementType,
                inputValue: updatedValue,
                isDirty: true,
            },
        };

        onElementBlur(newElementData, [elementId, ...(triggeringElementIds ?? [])]);
    }, [value, elementData, onElementDataChange, elementId, elementType]);

    const Component: ComponentType<BaseViewProps<typeof element, any>> | null = useMemo(() => Views[element.type], [element.type]);

    const isVisible = useMemo(() => {
        if (isAnyInputElement(element) && element.technical && (mode !== 'editor' || !disableVisibility)) {
            return false;
        }

        return elementDataObject.isVisible ?? true;
    }, [elementDataObject, element, mode, disableVisibility]);

    const viewProps: BaseViewProps<typeof element, any> = useMemo(() => ({
        allElements: allElements,
        element: element,
        setValue: handleSetValue,
        onBlur: handleOnBlur,
        errors: error,
        value: value,
        scrollContainerRef: scrollContainerRef,
        isBusy: isBusy,
        isDeriving: isDeriving,
        mode: mode,
        elementData: elementData,
        onElementDataChange: onElementDataChange,
        onElementBlur: onElementBlur,
        disableVisibility: disableVisibility,
        derivationTriggerIdQueue: derivationTriggerIdQueue,
    }), [
        allElements,
        element,
        handleSetValue,
        handleOnBlur,
        error,
        value,
        scrollContainerRef,
        isBusy,
        isDeriving,
        mode,
        elementData,
        onElementDataChange,
        onElementBlur,
        disableVisibility,
        derivationTriggerIdQueue,
    ]);

    if (Component == null || !isVisible) {
        return null;
    }

    return (
        <Grid
            item
            xs={12}
            md={('weight' in element && element.weight != null) ? element.weight : 12}
            id={elementId}
            data-initial-id={elementId /* TODO: Remove here and where referenced */}
            data-resolved-id={elementId /* TODO: Remove here and where referenced */}
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

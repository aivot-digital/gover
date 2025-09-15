import React, {ComponentType, useCallback, useMemo} from 'react';
import Grid from '@mui/material/Grid';
import {type AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {views as Views} from '../views';
import {type BaseViewProps} from '../views/base-view';
import {ElementErrorBoundary} from './element-error-boundary/element-error-boundary';
import {type ElementData, type ElementDataObject} from '../models/element-data';
import {resolveErrors, resolveOverride, resolvePrefill, resolveValueForResolvedOverride, resolveVisibility} from '../utils/element-data-utils';
import {ElementType} from '../data/element-type/element-type';

interface DispatcherComponentProps<T extends AnyElement> {
    rootElement: AnyElement;
    allElements: AnyElement[];
    element: T;

    scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
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
        rootElement,
        element: initialElement,
        allElements,
        scrollContainerRef,
        isBusy: baseIsBusy,
        isDeriving: baseIsDeriving,
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

    const elementDataObject: ElementDataObject = useMemo(() => {
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
        return resolveOverride(initialElement, elementData) as AnyElement;
    }, [initialElement, elementData]);

    const value = useMemo(() => {
        return resolveValueForResolvedOverride(element, elementData);
    }, [element, elementData]);

    const prefilled = useMemo(() => {
        return resolvePrefill(element, elementData);
    }, [element, elementData]);

    const error: string[] | undefined | null = useMemo(() => {
        return resolveErrors(element, elementData);
    }, [element, elementData]);

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

        return resolveVisibility(element, elementData);
    }, [elementData, element, mode, disableVisibility]);

    const isBusy: boolean = useMemo(() => {
        return baseIsBusy || baseIsDeriving && (
            (element.visibility?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false) ||
            (element.override?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false) ||
            (isAnyInputElement(element) && (element.value?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false))
        );
    }, [baseIsBusy, baseIsDeriving, element]);

    const viewProps: BaseViewProps<typeof element, any> = useMemo(() => ({
        rootElement: rootElement,
        allElements: allElements,
        element: element,
        setValue: handleSetValue,
        onBlur: handleOnBlur,
        errors: error,
        value: value,
        scrollContainerRef: scrollContainerRef,
        isBusy: isBusy || prefilled,
        isDeriving: baseIsDeriving,
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
        baseIsDeriving,
        prefilled,
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
            id={elementId}
            data-initial-id={elementId /* TODO: Remove here and where referenced */}
            data-resolved-id={elementId /* TODO: Remove here and where referenced */}
            sx={{
                position: 'relative',
            }}
            size={{
                xs: 12,
                md: ('weight' in element && element.weight != null) ? element.weight : 12
            }}>
            <ElementErrorBoundary viewProps={viewProps}>
                <Component {...viewProps} />
            </ElementErrorBoundary>
        </Grid>
    );
}

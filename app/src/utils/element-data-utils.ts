import {ElementDataObject, ElementData} from '../models/element-data';
import {AnyElement} from '../models/elements/any-element';
import {ElementType} from '../data/element-type/element-type';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {isReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';

export function resolveOverride(originalElement: AnyElement, data: ElementData): AnyElement {
    const elementId = originalElement.id;
    const elementDataObject: ElementDataObject | undefined = data[elementId];
    if (elementDataObject && elementDataObject.computedOverride) {
        return elementDataObject.computedOverride;
    }
    return originalElement;
}

export function resolveValue(originalElement: AnyElement, data: ElementData): any | undefined {
    const element = resolveOverride(originalElement, data);
    const elementId = element.id;
    const elementDataObject: ElementDataObject | undefined = data[elementId];

    if (elementDataObject) {
        if (isAnyInputElement(element) && (element.disabled || element.technical)) {
            return elementDataObject.computedValue;
        }

        if (elementDataObject.isDirty) {
            return elementDataObject.inputValue;
        }

        return elementDataObject.inputValue ?? elementDataObject.computedValue;
    }

    return undefined;
}

export interface MergeOptions {
    dontOverwriteErrors?: boolean;
}

export function mergeDerivedElementDataWithLocal(derivedElementData: ElementData, localElementData: ElementData, rootElement: AnyElement, options: MergeOptions): ElementData {
    const {
        id: elementId,
        type: elementType,
    } = rootElement;

    const derivedElementDataObject: ElementDataObject = derivedElementData[elementId] || {
        $type: elementType,
        computedErrors: undefined,
        isVisible: true,
        computedValue: undefined,
        inputValue: undefined,
        computedOverride: undefined,
        isDirty: false,
        isPrefilled: false,
    };

    const localElementDataObject: ElementDataObject = localElementData[elementId] || {
        $type: elementType,
        computedErrors: undefined,
        isVisible: true,
        computedValue: undefined,
        inputValue: undefined,
        computedOverride: undefined,
        isDirty: false,
        isPrefilled: false,
    };

    const mergedElementDataObject: ElementDataObject = {
        ...localElementDataObject,
        isVisible: derivedElementDataObject.isVisible,
        computedValue: derivedElementDataObject.computedValue,
        computedErrors: options.dontOverwriteErrors ? localElementDataObject.computedErrors : derivedElementDataObject.computedErrors,
        computedOverride: derivedElementDataObject.computedOverride,
    };

    let mergedElementData: ElementData = {
        [elementId]: mergedElementDataObject,
    };

    if (isAnyElementWithChildren(rootElement) && rootElement.children != null) {
        if (rootElement.type === ElementType.ReplicatingContainer) {
            const localChildInputValue = localElementDataObject.inputValue as ElementData[] | null | undefined;
            const derivedChildInputValue = derivedElementDataObject.inputValue as ElementData[] | null | undefined;

            if (localChildInputValue != null) {
                for (let i = 0; i < localChildInputValue.length; i++) {
                    let localChildElementData = localChildInputValue[i];
                    const derivedChildElementData: ElementData | undefined = derivedChildInputValue != null ? derivedChildInputValue[i] : undefined;

                    for (const childElement of rootElement.children) {
                        const childMergedData = mergeDerivedElementDataWithLocal(
                            derivedChildElementData,
                            localChildElementData,
                            childElement,
                            options,
                        );

                        localChildElementData = {
                            ...localChildElementData,
                            ...childMergedData,
                        }
                    }

                    localChildInputValue[i] = localChildElementData;
                }
            }
        } else {
            for (const childElement of rootElement.children) {
                const childMergedData = mergeDerivedElementDataWithLocal(
                    derivedElementData,
                    localElementData,
                    childElement,
                    options,
                );

                mergedElementData = {
                    ...mergedElementData,
                    ...childMergedData,
                };
            }
        }
    }

    return mergedElementData;
}

export function walkElementData(
    currentElement: AnyElement,
    currentElementData: ElementData,
    callback: (elem: AnyElement, value: any | null | undefined) => void,
): void {
    const val = resolveValue(currentElement, currentElementData);
    callback(currentElement, val);

    if (isReplicatingContainerLayout(currentElement)) {
        if (Array.isArray(val)) {
            for (const childElementData of val || []) {
                for (const child of currentElement.children || []) {
                    walkElementData(child, childElementData, callback);
                }
            }
        }
    } else if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children || []) {
            walkElementData(child, currentElementData, callback);
        }
    }
}
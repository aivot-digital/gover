import {
    AuthoredElementValues,
    ComputedElementState,
    ComputedElementSubState,
    ComputedElementStates,
    ComputedElementValueSource,
    createComputedElementSubState,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    EffectiveElementValues,
    isAuthoredElementValues,
    isReplicatingContainerElementValue,
    resolveReplicatingContainerElementValues,
    resolveComputedElementSubState,
    resolveComputedElementSubStateStates,
    type ReplicatingContainerElementValue,
    updateReplicatingContainerElementValues,
} from '../models/element-data';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {isReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {ElementType} from '../data/element-type/element-type';

export function resolveElementState(element: AnyElement, derivedData: DerivedRuntimeElementData): ComputedElementState | undefined {
    return derivedData.elementStates[element.id];
}

export function resolveOverride(originalElement: AnyElement, derivedData: DerivedRuntimeElementData): AnyElement {
    return resolveElementState(originalElement, derivedData)?.override ?? originalElement;
}

export function resolveValue(
    originalElement: AnyElement,
    authoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
): any | undefined | null {
    const element = resolveOverride(originalElement, derivedData);
    return resolveValueForResolvedOverride(element, authoredElementValues, derivedData);
}

export function resolveValueForResolvedOverride(
    element: AnyElement,
    authoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
): any | undefined | null {
    const elementState = resolveElementState(element, derivedData);
    const effectiveValue = derivedData.effectiveValues[element.id];
    const authoredValue = authoredElementValues[element.id];
    const hasAuthoredValue = Object.prototype.hasOwnProperty.call(authoredElementValues, element.id);

    if (isAnyInputElement(element) && (element.disabled || element.technical)) {
        return effectiveValue;
    }

    if (elementState?.valueSource === ComputedElementValueSource.Identity) {
        return effectiveValue;
    }

    if (hasAuthoredValue) {
        return authoredValue;
    }

    if (elementState?.valueSource === ComputedElementValueSource.Derived) {
        return effectiveValue;
    }

    return authoredValue;
}

export function resolveDisabled(element: AnyElement, derivedData: DerivedRuntimeElementData): boolean {
    const disabled = resolveElementState(element, derivedData)?.disabled;
    return disabled ?? false;
}

export function resolveErrors(element: AnyElement, derivedData: DerivedRuntimeElementData): string[] | undefined | null {
    const error = resolveElementState(element, derivedData)?.error;
    if (error == null || error.length === 0) {
        return undefined;
    }

    return [error];
}

export function resolveErrorDetails(element: AnyElement, derivedData: DerivedRuntimeElementData): any | undefined | null {
    const errorDetails = resolveElementState(element, derivedData)?.errorDetails;
    if (errorDetails == null) {
        return undefined;
    }

    return errorDetails;
}

export function resolveVisibility(element: AnyElement, derivedData: DerivedRuntimeElementData): boolean {
    return resolveElementState(element, derivedData)?.visible ?? true;
}

export function resolveReplicatingContainerItemDerivedData(
    element: AnyElement,
    derivedData: DerivedRuntimeElementData,
    index: number,
): DerivedRuntimeElementData {
    const rowEffectiveValues = derivedData.effectiveValues[element.id];
    const rowElementStates = resolveElementState(element, derivedData)?.subStates;
    const rowValue = Array.isArray(rowEffectiveValues) ? rowEffectiveValues[index] : null;
    const rowId = isReplicatingContainerElementValue(rowValue) ? rowValue.id : null;
    const rowValues = Array.isArray(rowEffectiveValues) ?
        resolveReplicatingContainerElementValues(rowValue) :
        null;
    const rowSubState = resolveComputedElementSubState(rowElementStates, rowId, index);

    return createDerivedRuntimeElementData({
        effectiveValues: rowValues != null ? rowValues as EffectiveElementValues : {},
        elementStates: resolveComputedElementSubStateStates(rowSubState),
    });
}

export function walkAuthoredElementValues(
    currentElement: AnyElement,
    currentElementValues: AuthoredElementValues,
    callback: (element: AnyElement, value: any | null | undefined) => void,
): void {
    const value = currentElementValues[currentElement.id];
    callback(currentElement, value);

    if (isReplicatingContainerLayout(currentElement)) {
        if (Array.isArray(value)) {
            for (const row of value) {
                const childElementValues = resolveReplicatingContainerElementValues(row);
                if (childElementValues == null) {
                    continue;
                }

                for (const child of currentElement.children || []) {
                    walkAuthoredElementValues(child, childElementValues, callback);
                }
            }
        }

        return;
    }

    if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children || []) {
            walkAuthoredElementValues(child, currentElementValues, callback);
        }
    }
}

export function mapAuthoredElementValues(
    currentElement: AnyElement,
    currentElementValues: AuthoredElementValues,
    callback: (element: AnyElement, value: any | null | undefined, path: Array<AnyElement | number>) => any | undefined,
    parents: Array<AnyElement | number> = [],
): AuthoredElementValues {
    const currentValue = currentElementValues[currentElement.id];
    const mappedValue = callback(currentElement, currentValue, parents);

    let mappedElementValues: AuthoredElementValues = {
        ...currentElementValues,
    };

    if (mappedValue === undefined) {
        delete mappedElementValues[currentElement.id];
    } else {
        mappedElementValues[currentElement.id] = mappedValue;
    }

    const nextCurrentValue = mappedValue === undefined ? currentValue : mappedValue;

    if (isReplicatingContainerLayout(currentElement)) {
        if (Array.isArray(nextCurrentValue)) {
            const mappedChildValues = nextCurrentValue.map((row, index) => {
                const childValues = resolveReplicatingContainerElementValues(row);
                if (!isAuthoredElementValues(childValues)) {
                    return row;
                }

                let updatedChildValues = {
                    ...childValues,
                };

                for (const child of currentElement.children || []) {
                    updatedChildValues = mapAuthoredElementValues(child, updatedChildValues, callback, [...parents, currentElement, index]);
                }

                return updateReplicatingContainerElementValues(row, updatedChildValues);
            });

            mappedElementValues = {
                ...mappedElementValues,
                [currentElement.id]: mappedChildValues,
            };
        }

        return mappedElementValues;
    }

    if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children || []) {
            mappedElementValues = mapAuthoredElementValues(child, mappedElementValues, callback, [...parents, currentElement]);
        }
    }

    return mappedElementValues;
}

export function filterAuthoredElementValues(
    currentElement: AnyElement,
    currentElementValues: AuthoredElementValues,
    callback: (element: AnyElement, value: any | null | undefined, path: Array<AnyElement | number>) => boolean,
    parents: Array<AnyElement | number> = [],
): AuthoredElementValues {
    const currentValue = currentElementValues[currentElement.id];
    const shouldKeepCurrentValue = callback(currentElement, currentValue, parents);

    let filteredValues: AuthoredElementValues = {};

    if (shouldKeepCurrentValue && currentValue !== undefined) {
        filteredValues[currentElement.id] = currentValue;
    }

    if (isReplicatingContainerLayout(currentElement)) {
        if (Array.isArray(currentValue)) {
            const filteredChildValues = currentValue
                .map((row, index) => {
                    const childValues = resolveReplicatingContainerElementValues(row);
                    if (!isAuthoredElementValues(childValues)) {
                        return undefined;
                    }

                    let filteredChildValue: AuthoredElementValues = {};
                    for (const child of currentElement.children || []) {
                        filteredChildValue = {
                            ...filteredChildValue,
                            ...filterAuthoredElementValues(child, childValues, callback, [...parents, currentElement, index]),
                        };
                    }

                    return Object.keys(filteredChildValue).length > 0 ?
                        updateReplicatingContainerElementValues(row, filteredChildValue) :
                        undefined;
                })
                .filter((childValues): childValues is ReplicatingContainerElementValue => childValues != null);

            if (filteredChildValues.length > 0) {
                filteredValues[currentElement.id] = filteredChildValues;
            } else {
                delete filteredValues[currentElement.id];
            }
        }

        return filteredValues;
    }

    if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children || []) {
            filteredValues = {
                ...filteredValues,
                ...filterAuthoredElementValues(child, currentElementValues, callback, [...parents, currentElement]),
            };
        }
    }

    return filteredValues;
}

export function cleanAuthoredElementValues(rootElement: AnyElement, authoredElementValues: AuthoredElementValues): AuthoredElementValues {
    const cleanedElementValues = {
        ...authoredElementValues,
    };

    delete cleanedElementValues[IdentityCustomerInputKey];

    return mapAuthoredElementValues(rootElement, cleanedElementValues, (element, value) => {
        if (
            element.type === ElementType.FileUpload ||
            element.type === ElementType.IdentityConfigElement ||
            element.type === ElementType.PaymentConfigElement
        ) {
            return undefined;
        }

        return value;
    });
}

export function normalizeReplicatingContainerValues(rootElement: AnyElement, authoredElementValues: AuthoredElementValues): AuthoredElementValues {
    return mapAuthoredElementValues(rootElement, authoredElementValues, (_, value) => value);
}


export function filterComputedElementStates(
    currentElement: AnyElement,
    currentElementValues: ComputedElementStates,
    callback: (element: AnyElement, value: ComputedElementState | null | undefined, path: Array<AnyElement | number>) => boolean,
    parents: Array<AnyElement | number> = [],
): ComputedElementStates {
    const currentElementState = currentElementValues[currentElement.id];
    const shouldKeepCurrentValue = callback(currentElement, currentElementState, parents);

    let filteredValues: ComputedElementStates = {};

    if (shouldKeepCurrentValue && currentElementState !== undefined) {
        filteredValues[currentElement.id] = currentElementState;
    }

    if (isReplicatingContainerLayout(currentElement)) {
        const filteredSubStates = (currentElementState?.subStates ?? [])
            .map((subState, index) => {
                const childValues = resolveComputedElementSubStateStates(subState);
                let filteredChildValue: ComputedElementStates = {};

                for (const child of currentElement.children || []) {
                    filteredChildValue = {
                        ...filteredChildValue,
                        ...filterComputedElementStates(child, childValues, callback, [...parents, currentElement, index]),
                    };
                }

                return Object.keys(filteredChildValue).length > 0 ? createComputedElementSubState(subState.id, filteredChildValue) : undefined;
            })
            .filter((childValues): childValues is ComputedElementSubState => childValues != null);

        if (filteredSubStates.length > 0) {
            filteredValues[currentElement.id] = {
                ...currentElementState,
                subStates: filteredSubStates
            };
        } else {
            delete filteredValues[currentElement.id];
        }

        return filteredValues;
    }

    if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children || []) {
            filteredValues = {
                ...filteredValues,
                ...filterComputedElementStates(child, currentElementValues, callback, [...parents, currentElement]),
            };
        }
    }

    return filteredValues;
}

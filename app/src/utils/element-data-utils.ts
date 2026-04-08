import {
    AuthoredElementValues,
    ComputedElementState,
    ComputedElementStates,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    EffectiveElementValues,
    isAuthoredElementValues,
    isElementStates,
} from '../models/element-data';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {isReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {ElementType} from '../data/element-type/element-type';
import {deepEquals} from './equality-utils';

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

    if (isAnyInputElement(element) && (element.disabled || element.technical)) {
        return effectiveValue;
    }

    if (elementState?.valueSource === 'Derived') {
        return effectiveValue;
    }

    return authoredValue ?? effectiveValue;
}

export function resolveErrors(element: AnyElement, derivedData: DerivedRuntimeElementData): string[] | undefined | null {
    const error = resolveElementState(element, derivedData)?.error;
    if (error == null || error.length === 0) {
        return undefined;
    }

    return [error];
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

    return createDerivedRuntimeElementData({
        effectiveValues: Array.isArray(rowEffectiveValues) && isAuthoredElementValues(rowEffectiveValues[index]) ?
            rowEffectiveValues[index] as EffectiveElementValues :
            {},
        elementStates: Array.isArray(rowElementStates) && isAuthoredElementValues(rowElementStates[index]) ?
            rowElementStates[index] as ComputedElementStates :
            {},
    });
}

export interface DestinationPathSyncResult {
    authoredElementValues: AuthoredElementValues;
    triggeringElementIds: string[];
}

/**
 * Mirrors a user edit to all authored-controlled inputs that currently resolve to the same
 * destination path inside the derived subtree.
 * <p>
 * Destination-path synchronization has to be driven by the concrete edited element instance rather
 * than by the schema element id alone. Replicating containers reuse the same child ids in every row,
 * but each row receives its own resolved destination path. Comparing the previous and next authored
 * values against the matching computed state lets the frontend identify the exact edited instance
 * before propagating the new value to its path-equivalent peers.
 */
export function synchronizeAuthoredElementValuesByDestinationPath(
    rootElement: AnyElement,
    previousAuthoredElementValues: AuthoredElementValues,
    nextAuthoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
    triggeringElementIds: string[],
): DestinationPathSyncResult {
    const sourceElementId = triggeringElementIds[0];
    if (sourceElementId == null) {
        return {
            authoredElementValues: nextAuthoredElementValues,
            triggeringElementIds,
        };
    }

    let sourceDestinationPath: string | null | undefined;
    let sourceNextValue: any | undefined;

    walkAuthoredElementValuesWithStates(
        rootElement,
        previousAuthoredElementValues,
        nextAuthoredElementValues,
        derivedData.elementStates,
        (element, previousValue, nextValue, state) => {
            if (sourceDestinationPath != null) {
                return;
            }

            if (!isAnyInputElement(element) || element.id !== sourceElementId) {
                return;
            }

            const destinationPath = state?.destinationPath;
            if (destinationPath == null || state?.valueSource === 'Derived') {
                return;
            }

            if (deepEquals(previousValue, nextValue)) {
                return;
            }

            sourceDestinationPath = destinationPath;
            sourceNextValue = nextValue;
        },
    );

    if (sourceDestinationPath == null) {
        return {
            authoredElementValues: nextAuthoredElementValues,
            triggeringElementIds,
        };
    }

    const synchronizedTriggeringElementIds = new Set(triggeringElementIds);
    const synchronizedAuthoredElementValues = mapAuthoredElementValuesWithStates(
        rootElement,
        nextAuthoredElementValues,
        derivedData.elementStates,
        (element, value, state) => {
            if (!isAnyInputElement(element)) {
                return value;
            }

            if (state?.destinationPath !== sourceDestinationPath || state?.valueSource === 'Derived') {
                return value;
            }

            if (deepEquals(value, sourceNextValue)) {
                return value;
            }

            // Adding the synchronized element id to the trigger set ensures that derivation rules
            // referencing the mirrored field are re-evaluated in the same update cycle.
            synchronizedTriggeringElementIds.add(element.id);
            return sourceNextValue;
        },
    );

    return {
        authoredElementValues: synchronizedAuthoredElementValues,
        triggeringElementIds: Array.from(synchronizedTriggeringElementIds),
    };
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
            for (const childElementValues of value) {
                if (!isAuthoredElementValues(childElementValues)) {
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
            const mappedChildValues = nextCurrentValue.map((childValues, index) => {
                if (!isAuthoredElementValues(childValues)) {
                    return childValues;
                }

                let updatedChildValues = {
                    ...childValues,
                };

                for (const child of currentElement.children || []) {
                    updatedChildValues = mapAuthoredElementValues(child, updatedChildValues, callback, [...parents, currentElement, index]);
                }

                return updatedChildValues;
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
                .map((childValues, index) => {
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

                    return Object.keys(filteredChildValue).length > 0 ? filteredChildValue : undefined;
                })
                .filter((childValues): childValues is AuthoredElementValues => childValues != null);

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
        if (element.type === ElementType.FileUpload) {
            return undefined;
        }

        return value;
    });
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
            .map((childValues, index) => {
                let filteredChildValue: ComputedElementStates = {};

                for (const child of currentElement.children || []) {
                    filteredChildValue = {
                        ...filteredChildValue,
                        ...filterComputedElementStates(child, childValues, callback, [...parents, currentElement, index]),
                    };
                }

                return Object.keys(filteredChildValue).length > 0 ? filteredChildValue : undefined;
            })
            .filter((childValues): childValues is AuthoredElementValues => childValues != null);

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

/**
 * Traverses authored values and computed states in lock-step.
 * <p>
 * Replicating containers split authored values into row-local maps while their computed metadata is
 * stored in `subStates`. Consumers that need to reason about the currently resolved runtime path of
 * a concrete authored value therefore need both structures at the same time.
 */
function walkAuthoredElementValuesWithStates(
    currentElement: AnyElement,
    previousElementValues: AuthoredElementValues,
    nextElementValues: AuthoredElementValues,
    currentElementStates: ComputedElementStates,
    callback: (
        element: AnyElement,
        previousValue: any | null | undefined,
        nextValue: any | null | undefined,
        state: ComputedElementState | null | undefined,
        path: Array<AnyElement | number>,
    ) => void,
    parents: Array<AnyElement | number> = [],
): void {
    const currentElementState = currentElementStates[currentElement.id];
    const previousValue = previousElementValues[currentElement.id];
    const nextValue = nextElementValues[currentElement.id];

    callback(currentElement, previousValue, nextValue, currentElementState, parents);

    if (isReplicatingContainerLayout(currentElement)) {
        const previousRows = Array.isArray(previousValue) ? previousValue : [];
        const nextRows = Array.isArray(nextValue) ? nextValue : [];
        const childElementStates = Array.isArray(currentElementState?.subStates) ? currentElementState.subStates : [];
        const rowCount = Math.max(previousRows.length, nextRows.length, childElementStates.length);

        for (let index = 0; index < rowCount; index++) {
            const previousRowValues = isAuthoredElementValues(previousRows[index]) ? previousRows[index] : {};
            const nextRowValues = isAuthoredElementValues(nextRows[index]) ? nextRows[index] : {};
            const rowStates = isElementStates(childElementStates[index]) ? childElementStates[index] : {};

            for (const child of currentElement.children || []) {
                walkAuthoredElementValuesWithStates(
                    child,
                    previousRowValues,
                    nextRowValues,
                    rowStates,
                    callback,
                    [...parents, currentElement, index],
                );
            }
        }

        return;
    }

    if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children || []) {
            walkAuthoredElementValuesWithStates(
                child,
                previousElementValues,
                nextElementValues,
                currentElementStates,
                callback,
                [...parents, currentElement],
            );
        }
    }
}

/**
 * Maps authored values while preserving access to the matching computed state.
 * <p>
 * Destination-path synchronization needs to rewrite the authored tree based on runtime metadata.
 * Keeping the authored and computed traversals aligned in one helper prevents row-local updates in
 * replicating containers from drifting away from the `subStates` that describe their resolved paths.
 */
function mapAuthoredElementValuesWithStates(
    currentElement: AnyElement,
    currentElementValues: AuthoredElementValues,
    currentElementStates: ComputedElementStates,
    callback: (
        element: AnyElement,
        value: any | null | undefined,
        state: ComputedElementState | null | undefined,
        path: Array<AnyElement | number>,
    ) => any | undefined,
    parents: Array<AnyElement | number> = [],
): AuthoredElementValues {
    const currentElementState = currentElementStates[currentElement.id];
    const currentValue = currentElementValues[currentElement.id];
    const mappedValue = callback(currentElement, currentValue, currentElementState, parents);

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
            const childElementStates = Array.isArray(currentElementState?.subStates) ? currentElementState.subStates : [];
            const mappedChildValues = nextCurrentValue.map((childValues, index) => {
                if (!isAuthoredElementValues(childValues)) {
                    return childValues;
                }

                let updatedChildValues = {
                    ...childValues,
                };
                const rowStates = isElementStates(childElementStates[index]) ? childElementStates[index] : {};

                for (const child of currentElement.children || []) {
                    updatedChildValues = mapAuthoredElementValuesWithStates(
                        child,
                        updatedChildValues,
                        rowStates,
                        callback,
                        [...parents, currentElement, index],
                    );
                }

                return updatedChildValues;
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
            mappedElementValues = mapAuthoredElementValuesWithStates(
                child,
                mappedElementValues,
                currentElementStates,
                callback,
                [...parents, currentElement],
            );
        }
    }

    return mappedElementValues;
}

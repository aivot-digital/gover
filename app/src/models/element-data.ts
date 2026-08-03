import type {AnyElement} from './elements/any-element';
import {isStringNotNullOrEmpty} from '../utils/string-utils';
import {type AnyElementWithChildren, isAnyElementWithChildren} from './elements/any-element-with-children';
import {isReplicatingContainerLayout} from './elements/form/layout/replicating-container-layout';

export type AuthoredElementValues = Partial<Record<string, any>>;

export type EffectiveElementValues = Partial<Record<string, any>>;

export interface ReplicatingContainerElementValue {
    id?: string | null;
    values?: AuthoredElementValues | null;
}

export type ReplicatingContainerElementValues = ReplicatingContainerElementValue[];

export enum ComputedElementValueSource {
    Authored = 'Authored',
    Derived = 'Derived',
    Identity = 'Identity',
}

export interface ComputedElementState {
    visible?: boolean | null;
    disabled?: boolean | null;
    error?: string | null;
    errorDetails?: Record<string, any> | null;
    override?: AnyElement | null;
    valueSource?: ComputedElementValueSource | null;
    subStates?: ComputedElementSubState[] | null;
}

export type ComputedElementStates = Partial<Record<string, ComputedElementState>>;

export interface ComputedElementSubState {
    id?: string | null;
    states?: ComputedElementStates | null;
}

export type ComputedElementError = Pick<ComputedElementState, 'error' | 'errorDetails' | 'subStates'>;

export type ComputedElementErrors = Partial<Record<string, ComputedElementError>>;

export interface DerivedRuntimeElementData {
    effectiveValues: EffectiveElementValues;
    elementStates: ComputedElementStates;
}

export function createDerivedRuntimeElementData(data?: Partial<DerivedRuntimeElementData>): DerivedRuntimeElementData {
    return {
        effectiveValues: {},
        elementStates: {},
        ...data,
    };
}

export function isAuthoredElementValues(obj: any): obj is AuthoredElementValues {
    return obj != null && typeof obj === 'object' && !Array.isArray(obj);
}

export function isReplicatingContainerElementValue(obj: any): obj is ReplicatingContainerElementValue {
    if (!isAuthoredElementValues(obj)) {
        return false;
    }

    const keys = Object.keys(obj);
    return keys.length > 0 &&
        keys.every((key) => key === 'id' || key === 'values') &&
        (obj.values == null || isAuthoredElementValues(obj.values));
}

export function resolveReplicatingContainerElementValues(row: any): AuthoredElementValues | null {
    if (isReplicatingContainerElementValue(row)) {
        return row.values ?? {};
    }

    return isAuthoredElementValues(row) ? row : null;
}

export function updateReplicatingContainerElementValues(row: any, values: AuthoredElementValues): ReplicatingContainerElementValue {
    return isReplicatingContainerElementValue(row) ?
        {...row, values} :
        {values};
}

export function resolveComputedElementSubStateStates(subState: ComputedElementSubState | ComputedElementStates | null | undefined): ComputedElementStates {
    if (subState == null) {
        return {};
    }

    if (isComputedElementSubState(subState)) {
        return subState.states ?? {};
    }

    return subState as ComputedElementStates;
}

export function resolveComputedElementSubState(
    subStates: Array<ComputedElementSubState | ComputedElementStates> | null | undefined,
    id: string | null | undefined,
    index: number,
): ComputedElementSubState | ComputedElementStates | null {
    if (subStates == null) {
        return null;
    }

    if (id != null) {
        const matchingSubState = subStates.find((subState) => (
            isComputedElementSubState(subState) &&
            subState.id === id
        ));
        if (matchingSubState != null) {
            return matchingSubState;
        }
    }

    return index >= 0 && index < subStates.length ? subStates[index] : null;
}

export function createComputedElementSubState(id: string | null | undefined, states: ComputedElementStates): ComputedElementSubState {
    return {
        id: id ?? null,
        states,
    };
}

export function isEffectiveValues(obj: any): obj is EffectiveElementValues {
    return obj != null && typeof obj === 'object' && !Array.isArray(obj);
}

export function isElementStates(obj: any): obj is ComputedElementStates {
    return obj != null && typeof obj === 'object' && !Array.isArray(obj);
}

export function isComputedElementSubState(obj: any): obj is ComputedElementSubState {
    return obj != null &&
        typeof obj === 'object' &&
        !Array.isArray(obj) &&
        (
            Object.prototype.hasOwnProperty.call(obj, 'states') ||
            Object.prototype.hasOwnProperty.call(obj, 'id')
        ) &&
        (obj.states == null || isElementStates(obj.states));
}

export function isDerivedRuntimeElementData(obj: any): obj is DerivedRuntimeElementData {
    return obj != null &&
        typeof obj === 'object' &&
        'effectiveValues' in obj &&
        'elementStates' in obj &&
        isEffectiveValues(obj.effectiveValues) &&
        isElementStates(obj.elementStates);
}

export interface ElementDerivationLogItem {
    timestamp: string;
    level: 'Debug' | 'Error';
    elementId: string;
    message: string;
    details: Record<string, any>;
}

export interface ElementDerivationResponse {
    elementData: DerivedRuntimeElementData;
    logItems: ElementDerivationLogItem[];
}

export function hasAuthoredElementValuesSomeInput(authoredElementValues: AuthoredElementValues | null | undefined): boolean {
    if (authoredElementValues == null) {
        return false;
    }

    for (const key of Object.keys(authoredElementValues)) {
        if (authoredElementValues[key] !== undefined) {
            return true;
        }
    }

    return false;
}

/**
 * Apply computed errors to a computed element state container.
 * The computed errors will be applied recursively to all sub-states of the computed element states.
 * Computed errors have a higher precedence than existing errors.
 *
 * @param {ComputedElementErrors} computedErrors The computed errors.
 * @param {ComputedElementStates} computedElementStates The existing element states.
 * @return {ComputedElementStates} The updated element states.
 */
export function applyComputedErrors(computedErrors: ComputedElementErrors, computedElementStates: ComputedElementStates): ComputedElementStates {
    const nextComputedElementStates: ComputedElementStates = {
        ...computedElementStates,
    };

    for (const [
        elementId,
        computedError,
    ] of Object.entries(computedErrors)) {
        if (computedError == null) {
            continue;
        }

        const previousState = computedElementStates[elementId];
        const nextState: ComputedElementState = {
            ...(previousState ?? {}),
        };

        if (Object.prototype.hasOwnProperty.call(computedError, 'error')) {
            nextState.error = computedError.error ?? null;
        }

        if (Object.prototype.hasOwnProperty.call(computedError, 'errorDetails')) {
            nextState.errorDetails = computedError.errorDetails ?? null;
        }

        if (Object.prototype.hasOwnProperty.call(computedError, 'subStates')) {
            const computedSubStates = computedError.subStates;

            nextState.subStates = computedSubStates == null ?
                computedSubStates ?? null :
                computedSubStates.map((computedSubState, index) => {
                    const previousSubState = resolveComputedElementSubState(previousState?.subStates, computedSubState?.id, index);
                    return createComputedElementSubState(
                        computedSubState?.id ?? (isComputedElementSubState(previousSubState) ? previousSubState.id : null),
                        applyComputedErrors(
                            resolveComputedElementSubStateStates(computedSubState),
                            resolveComputedElementSubStateStates(previousSubState),
                        ),
                    );
                });
        }

        nextComputedElementStates[elementId] = nextState;
    }

    return nextComputedElementStates;
}

export function clearDerivedErrorsRecursively(derivedData: DerivedRuntimeElementData): DerivedRuntimeElementData {
    return {
        ...derivedData,
        elementStates: Object.fromEntries(
            Object.entries(derivedData.elementStates).map(([
                elementId,
                state,
            ]) => [
                elementId,
                {
                    ...state,
                    error: null,
                    subStates: state?.subStates?.map((subState) => createComputedElementSubState(subState.id, clearDerivedErrorsRecursively({
                        effectiveValues: {},
                        elementStates: resolveComputedElementSubStateStates(subState),
                    }).elementStates)) ?? null,
                },
            ]),
        ),
    };
}

export function hasAnyErrorRecursively(elementStates: ComputedElementStates): boolean {
    return Object
        .keys(elementStates)
        .some((key) => {
            const state = elementStates[key];
            if (state == null) {
                return false;
            }

            if (isStringNotNullOrEmpty(state.error)) {
                return true;
            }

            if (state.subStates != null) {
                return state.subStates.some((subState) => hasAnyErrorRecursively(resolveComputedElementSubStateStates(subState)));
            }

            return false;
        });
}

export function hasAnyErrorRecursivelyInParent(parent: AnyElementWithChildren, allElementStates: ComputedElementStates): boolean {
    return hasAnyErrorRecursivelyInElement(parent, allElementStates);
}

function hasAnyErrorRecursivelyInElement(element: AnyElement, elementStates: ComputedElementStates): boolean {
    const state = elementStates[element.id];

    if (isStringNotNullOrEmpty(state?.error)) {
        return true;
    }

    if (isReplicatingContainerLayout(element)) {
        return state?.subStates?.some((subState) => {
            const subStateStates = resolveComputedElementSubStateStates(subState);
            return element.children?.some((child) => hasAnyErrorRecursivelyInElement(child, subStateStates)) ?? false;
        }) ?? false;
    }

    if (isAnyElementWithChildren(element)) {
        return element.children?.some((child) => hasAnyErrorRecursivelyInElement(child, elementStates)) ?? false;
    }

    return false;
}

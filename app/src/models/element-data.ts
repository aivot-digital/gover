import {AnyElement} from './elements/any-element';
import {isStringNotNullOrEmpty} from '../utils/string-utils';

export type AuthoredElementValues = Partial<Record<string, any>>;

export type EffectiveElementValues = Partial<Record<string, any>>;

export type ComputedElementValueSource = 'Authored' | 'Derived';

export interface ComputedElementState {
    visible?: boolean | null;
    error?: string | null;
    errorDetails?: Record<string, any> | null;
    override?: AnyElement | null;
    destinationPath?: string | null;
    valueSource?: ComputedElementValueSource | null;
    subStates?: ComputedElementStates[] | null;
}

export type ComputedElementStates = Partial<Record<string, ComputedElementState>>;

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

export function isEffectiveValues(obj: any): obj is EffectiveElementValues {
    return obj != null && typeof obj === 'object' && !Array.isArray(obj);
}

export function isElementStates(obj: any): obj is ComputedElementStates {
    return obj != null && typeof obj === 'object' && !Array.isArray(obj);
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
        if (authoredElementValues[key] != null) {
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
 * @param computedErrors The computed errors.
 * @param computedElementStates The existing element states.
 * @returns The updated element states.
 */
export function applyComputedErrors(computedErrors: ComputedElementErrors, computedElementStates: ComputedElementStates): ComputedElementStates {
    const nextComputedElementStates: ComputedElementStates = {
        ...computedElementStates,
    };

    for (const [elementId, computedError] of Object.entries(computedErrors)) {
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
                    return applyComputedErrors(computedSubState ?? {}, previousState?.subStates?.[index] ?? {});
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
            Object.entries(derivedData.elementStates).map(([elementId, state]) => [
                elementId,
                {
                    ...state,
                    error: null,
                    subStates: state?.subStates?.map((subState) => clearDerivedErrorsRecursively({
                        effectiveValues: {},
                        elementStates: subState ?? {},
                    }).elementStates) ?? null,
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
                return state.subStates.some((subState) => hasAnyErrorRecursively(subState ?? {}));
            }

            return false;
        });
}

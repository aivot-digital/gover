import {type AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from './string-utils';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {Function} from '../models/functions/function';

export type FunctionStatus = 'todo' | 'done' | 'unnecessary';

export interface FunctionStatusItem {
    func: string;
    status: FunctionStatus;
}

export function getFunctionStatus(comp: AnyElement): FunctionStatusItem[] {
    const results: FunctionStatusItem[] = [];

    if (comp.isVisible != null || comp.visibilityCode != null || comp.visibilityExpression != null) {
        const funcExists = isStringNotNullOrEmpty(comp.isVisible?.code) || comp.isVisible?.conditionSet != null || isStringNotNullOrEmpty(comp.visibilityCode?.code) || isStringNotNullOrEmpty(comp.visibilityExpression?.operatorIdentifier);
        if (isStringNotNullOrEmpty(comp.isVisible?.requirements)) {
            results.push({
                func: 'Sichtbarkeit',
                status: funcExists ? 'done' : 'todo',
            });
        } else if (funcExists) {
            results.push({
                func: 'Sichtbarkeit',
                status: 'unnecessary',
            });
        }
    }

    if (comp.patchElement != null || comp.overrideCode != null || comp.overrideExpression != null) {
        const funcExists = isStringNotNullOrEmpty(comp.patchElement?.code) || isStringNotNullOrEmpty(comp.overrideCode?.code) || isStringNotNullOrEmpty(comp.overrideExpression?.operatorIdentifier);
        if (isStringNotNullOrEmpty(comp.patchElement?.requirements)) {
            results.push({
                func: 'Struktur',
                status: funcExists ? 'done' : 'todo',
            });
        } else if (funcExists) {
            results.push({
                func: 'Struktur',
                status: 'unnecessary',
            });
        }
    }

    if (isAnyInputElement(comp)) {
        if (comp.validate != null || comp.validationCode != null || (comp.validationExpressions?.length ?? 0) > 0) {
            const funcExists = isStringNotNullOrEmpty(comp.validate?.code) || comp.validate?.conditionSet != null || isStringNotNullOrEmpty(comp.validationCode?.code) || (comp.validationExpressions?.length ?? 0) > 0;
            if (isStringNotNullOrEmpty(comp.validate?.requirements)) {
                results.push({
                    func: 'Validierung',
                    status: funcExists ? 'done' : 'todo',
                });
            } else if (funcExists) {
                results.push({
                    func: 'Validierung',
                    status: 'unnecessary',
                });
            }
        }

        if (comp.computeValue != null || comp.valueCode != null || comp.valueExpression != null) {
            const funcExists = isStringNotNullOrEmpty(comp.computeValue?.code) || isStringNotNullOrEmpty(comp.valueCode?.code) || isStringNotNullOrEmpty(comp.valueExpression?.operatorIdentifier);
            if (isStringNotNullOrEmpty(comp.computeValue?.requirements)) {
                results.push({
                    func: 'Wert',
                    status: funcExists ? 'done' : 'todo',
                });
            } else if (funcExists) {
                results.push({
                    func: 'Wert',
                    status: 'unnecessary',
                });
            }
        }
    }

    return results;
}


export enum FunctionType {
    VISIBILITY = 'visibility',
    OVERRIDE = 'override',
    VALIDATION = 'validation',
    VALUE = 'value',
}

export const FunctionTypeLabels: Record<FunctionType, string> = {
    [FunctionType.VISIBILITY]: 'Sichtbarkeit',
    [FunctionType.OVERRIDE]: 'Überschreibung',
    [FunctionType.VALIDATION]: 'Validierung',
    [FunctionType.VALUE]: 'Wertberechnung',
}

function checkLegacyFunctionExists(func?: Function): boolean {
    return isStringNotNullOrEmpty(func?.code) || func?.conditionSet != null;
}

export function hasElementFunctionType(element: AnyElement, type: FunctionType): boolean {
    switch (type) {
        case FunctionType.VISIBILITY:
            return checkLegacyFunctionExists(element.isVisible) || isStringNotNullOrEmpty(element.visibilityCode?.code) || isStringNotNullOrEmpty(element.visibilityExpression?.operatorIdentifier);
        case FunctionType.OVERRIDE:
            return checkLegacyFunctionExists(element.patchElement) || isStringNotNullOrEmpty(element.overrideCode?.code) || isStringNotNullOrEmpty(element.overrideExpression?.operatorIdentifier);
        case FunctionType.VALIDATION:
            if (isAnyInputElement(element)) {
                return checkLegacyFunctionExists(element.validate) || isStringNotNullOrEmpty(element.validate?.code) || (element.validationExpressions?.length ?? 0) > 0;
            } else {
                return false;
            }
        case FunctionType.VALUE:
            if (isAnyInputElement(element)) {
                return isStringNotNullOrEmpty(element.computeValue?.code) || isStringNotNullOrEmpty(element.valueCode?.code) || isStringNotNullOrEmpty(element.valueExpression?.operatorIdentifier);
            } else {
                return false;
            }
    }
}

export function checkStatus(element: AnyElement): FunctionType[] {
    const results: FunctionType[] = [];

    if (hasElementFunctionType(element, FunctionType.VISIBILITY)) {
        results.push(FunctionType.VISIBILITY);
    }

    if (hasElementFunctionType(element, FunctionType.OVERRIDE)) {
        results.push(FunctionType.OVERRIDE);
    }

    if (hasElementFunctionType(element, FunctionType.VALIDATION)) {
        results.push(FunctionType.VALIDATION);
    }

    if (hasElementFunctionType(element, FunctionType.VALUE)) {
        results.push(FunctionType.VALUE);
    }

    return results;
}

export function hasFunction(element: AnyElement): boolean {
    return checkStatus(element).length > 0;
}
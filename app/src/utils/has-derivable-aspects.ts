import {AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from './string-utils';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {JavascriptCode} from '../models/functions/javascript-code';
import {NoCodeOperand, ValidationExpressionWrapper} from '../models/functions/no-code-expression';
import {ConditionSet} from '../models/functions/conditions/condition-set';

export function hasDerivableAspects(element: AnyElement, ignoreValidationCode: boolean = true): boolean {
    if (extractVisibility(element) != null) {
        return true;
    }

    if (extractOverride(element) != null) {
        return true;
    }

    if (isAnyInputElement(element)) {
        if (!ignoreValidationCode && extractValidation(element) != null) {
            return true;
        }

        if (extractValue(element) != null) {
            return true;
        }
    }

    return false;
}

export function extractVisibility(element: AnyElement): ConditionSet | JavascriptCode | NoCodeOperand | undefined {
    const vis = element.visibility;

    if (vis == null) {
        return undefined;
    }

    if (vis.javascriptCode != null && isStringNotNullOrEmpty(vis.javascriptCode.code)) {
        return vis.javascriptCode;
    }

    if (vis.conditionSet != null) {
        return vis.conditionSet;
    }

    if (vis.noCode != null) {
        return vis.noCode;
    }

    return undefined;
}

export function extractOverride(element: AnyElement): ConditionSet | JavascriptCode | Record<string, NoCodeOperand> | undefined {
    var override = element.override;

    if (override == null) {
        return undefined;
    }

    if (override.javascriptCode != null && isStringNotNullOrEmpty(override.javascriptCode.code)) {
        return override.javascriptCode;
    }

    if (override.fieldNoCodeMap != null) {
        return override.fieldNoCodeMap;
    }

    return undefined;
}

export function extractValidation(element: AnyInputElement): ConditionSet | JavascriptCode | ValidationExpressionWrapper[] | undefined {
    const validation = element.validation;

    if (validation == null) {
        return undefined;
    }

    if (validation.javascriptCode != null && isStringNotNullOrEmpty(validation.javascriptCode.code)) {
        return validation.javascriptCode;
    }

    if (validation.conditionSet != null) {
        return validation.conditionSet;
    }

    if (validation.noCodeList != null && validation.noCodeList.length > 0) {
        return validation.noCodeList;
    }

    return undefined;
}

export function extractValue(element: AnyInputElement): ConditionSet | JavascriptCode | NoCodeOperand | undefined {
    const value = element.value;

    if (value == null) {
        return undefined;
    }

    if (value.javascriptCode != null && isStringNotNullOrEmpty(value.javascriptCode.code)) {
        return value.javascriptCode;
    }

    if (value.noCode != null) {
        return value.noCode;
    }

    return undefined;
}
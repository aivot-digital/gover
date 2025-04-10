import {AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from './string-utils';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {Function} from '../models/functions/function';
import {JavascriptCode} from '../models/functions/javascript-code';
import {NoCodeExpression, ValidationExpressionWrapper} from '../models/functions/no-code-expression';

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

export function extractVisibility(element: AnyElement): Function | JavascriptCode | NoCodeExpression | undefined {
    if (element.isVisible != null) {
        if (isStringNotNullOrEmpty(element.isVisible.code)) {
            return element.isVisible;
        }

        if (element.isVisible.conditionSet != null) {
            if (element.isVisible.conditionSet.conditions != null && element.isVisible.conditionSet.conditions.length > 0) {
                return element.isVisible;
            }

            if (element.isVisible.conditionSet.conditionsSets != null && element.isVisible.conditionSet.conditionsSets.length > 0) {
                return element.isVisible;
            }
        }
    }

    if (element.visibilityCode != null && isStringNotNullOrEmpty(element.visibilityCode.code)) {
        return element.visibilityCode;
    }

    if (element.visibilityExpression != null && isStringNotNullOrEmpty(element.visibilityExpression.operatorIdentifier)) {
        return element.visibilityExpression;
    }

    return undefined;
}

export function extractOverride(element: AnyElement): Function | JavascriptCode | NoCodeExpression | undefined {
    if (element.patchElement != null) {
        if (isStringNotNullOrEmpty(element.patchElement.code)) {
            return element.patchElement;
        }

        if (element.patchElement.conditionSet != null) {
            if (element.patchElement.conditionSet.conditions != null && element.patchElement.conditionSet.conditions.length > 0) {
                return element.patchElement;
            }

            if (element.patchElement.conditionSet.conditionsSets != null && element.patchElement.conditionSet.conditionsSets.length > 0) {
                return element.patchElement;
            }
        }
    }

    if (element.overrideCode != null && isStringNotNullOrEmpty(element.overrideCode.code)) {
        return element.overrideCode;
    }

    if (element.overrideExpression != null && isStringNotNullOrEmpty(element.overrideExpression.operatorIdentifier)) {
        return element.overrideExpression;
    }

    return undefined;
}

export function extractValidation(element: AnyInputElement): Function | JavascriptCode | ValidationExpressionWrapper[] | undefined {
    if (element.validate != null) {
        if (isStringNotNullOrEmpty(element.validate.code)) {
            return element.validate;
        }

        if (element.validate.conditionSet != null) {
            if (element.validate.conditionSet.conditions != null && element.validate.conditionSet.conditions.length > 0) {
                return element.computeValue;
            }

            if (element.validate.conditionSet.conditionsSets != null && element.validate.conditionSet.conditionsSets.length > 0) {
                return element.computeValue;
            }
        }
    }

    if (element.validationCode != null && isStringNotNullOrEmpty(element.validationCode.code)) {
        return element.validationCode;
    }

    if (element.validationExpressions != null && element.validationExpressions.length > 0) {
        return element.validationExpressions;
    }

    return undefined;
}

export function extractValue(element: AnyInputElement): Function | JavascriptCode | NoCodeExpression | undefined {
    if (element.computeValue != null) {
        if (isStringNotNullOrEmpty(element.computeValue.code)) {
            return element.computeValue;
        }

        if (element.computeValue.conditionSet != null) {
            if (element.computeValue.conditionSet.conditions != null && element.computeValue.conditionSet.conditions.length > 0) {
                return element.computeValue;
            }

            if (element.computeValue.conditionSet.conditionsSets != null && element.computeValue.conditionSet.conditionsSets.length > 0) {
                return element.computeValue;
            }
        }
    }

    if (element.valueCode != null && isStringNotNullOrEmpty(element.valueCode.code)) {
        return element.valueCode;
    }

    if (element.valueExpression != null && isStringNotNullOrEmpty(element.valueExpression.operatorIdentifier)) {
        return element.valueExpression;
    }

    return undefined;
}
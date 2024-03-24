import {type CustomerInput} from '../models/customer-input';
import {type AnyElement} from '../models/elements/any-element';
import {type ConditionSet} from '../models/functions/conditions/condition-set';
import {type Condition} from '../models/functions/conditions/condition';
import {ConditionSetOperator} from '../data/condition-set-operator';
import {ConditionOperatorMessage} from '../data/condition-operator';
import {isStringNotNullOrEmpty, stringOrDefault} from './string-utils';
import Evaluators from '../evaluators';
import {type Function as FunctionModel} from '../models/functions/function';
import {resolveId} from './id-utils';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {isElementVisible} from './is-element-visible';

export function evaluateFunction(
    idPrefix: string | undefined,
    allElements: AnyElement[],
    func: FunctionModel | undefined | null,
    customerInput: CustomerInput,
    element: AnyElement,
    id: string,
    returnBoolean: boolean,
): any {
    if (func == null) {
        return null;
    }

    if (isStringNotNullOrEmpty(func.code)) {
        return evaluateFunctionCode(func, customerInput, element, resolveId(id, idPrefix));
    } else {
        const ret = evaluateFunctionNoCode(idPrefix, allElements, func, customerInput, returnBoolean);
        return returnBoolean ? ret == null : ret;
    }
}

function evaluateFunctionCode(func: FunctionModel, customerInput: CustomerInput, element: AnyElement, id: string): any {
    const fn = new Function('data', 'element', 'id', `
            ${func.code}
            return main(data, element, id);
        `);
    try {
        return fn(customerInput, element, id);
    } catch (err) {
        console.error(`Failed to execute function for ${id}: ${err}`);
    }
    return null;
}

function evaluateFunctionNoCode(
    idPrefix: string | undefined,
    allElements: AnyElement[],
    func: FunctionModel,
    customerInput: CustomerInput,
    ignoreEmptyValues: boolean,
): string | null {
    return func.conditionSet != null ? evaluateConditionSet(idPrefix, allElements, func.conditionSet, customerInput, ignoreEmptyValues) : null;
}

function evaluateConditionSet(
    idPrefix: string | undefined,
    allElements: AnyElement[],
    conditionSet: ConditionSet,
    customerInput: CustomerInput,
    ignoreEmptyValues: boolean,
): string | null {
    switch (conditionSet.operator) {
        case ConditionSetOperator.All:
            if (conditionSet.conditions != null) {
                for (const cond of conditionSet.conditions) {
                    const res = evaluateCondition(idPrefix, allElements, cond, customerInput, ignoreEmptyValues);
                    if (res != null) {
                        return stringOrDefault(conditionSet.conditionSetUnmetMessage, res);
                    }
                }
            }
            if (conditionSet.conditionsSets != null) {
                for (const condSet of conditionSet.conditionsSets) {
                    const res = evaluateConditionSet(idPrefix, allElements, condSet, customerInput, ignoreEmptyValues);
                    if (res != null) {
                        return stringOrDefault(conditionSet.conditionSetUnmetMessage, res);
                    }
                }
            }
            return null;
        case ConditionSetOperator.Any:
            if (
                !(conditionSet.conditions ?? []).some((cond) => evaluateCondition(idPrefix, allElements, cond, customerInput, ignoreEmptyValues) == null) &&
                !(conditionSet.conditionsSets ?? []).some((condSet) => evaluateConditionSet(idPrefix, allElements, condSet, customerInput, ignoreEmptyValues) == null)
            ) {
                return stringOrDefault(conditionSet.conditionSetUnmetMessage, 'Keine der Bedingungen ist erfüllt');
            }
            return null;
    }
}

function evaluateCondition(idPrefix: string | undefined, allElements: AnyElement[], condition: Condition, customerInput: CustomerInput, ignoreEmptyValues: boolean): string | null {
    if (condition.operator == null) {
        return null;
    }

    const referencedElement = allElements.find((elem) => elem.id === condition.reference);
    if (referencedElement == null) {
        return null;
    }

    const evaluator = Evaluators[referencedElement.type];
    if (evaluator == null) {
        return null;
    }

    const operatorEvaluator = evaluator[condition.operator];
    if (operatorEvaluator == null) {
        return null;
    }

    const referencedElementIsVisible = isElementVisible(idPrefix, allElements, referencedElement.id, referencedElement, customerInput);
    const referenceValue = referencedElementIsVisible ? customerInput[resolveId(condition.reference, idPrefix)] : null;

    if (!ignoreEmptyValues && referenceValue == null && isAnyInputElement(referencedElement) && !referencedElement.required) {
        return null;
    }

    const targetElement = condition.target != null ? allElements.find((elem) => elem.id === condition.target) : null;
    const targetElementIsVisible = targetElement != null ? isElementVisible(idPrefix, allElements, targetElement.id, targetElement, customerInput) : false;

    const targetValue = targetElement != null ? (targetElementIsVisible ? customerInput[resolveId(targetElement.id, idPrefix)] : null) : condition.value ?? '';

    if (operatorEvaluator(referenceValue, targetValue)) {
        return null;
    } else {
        if (condition.conditionUnmetMessage != null && condition.conditionUnmetMessage !== '') {
            return condition.conditionUnmetMessage;
        } else {
            return ConditionOperatorMessage[condition.operator](referenceValue, targetValue);
        }
    }
}

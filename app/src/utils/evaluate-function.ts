import {CustomerInput} from "../models/customer-input";
import {AnyElement} from "../models/elements/any-element";
import {ConditionSet} from "../models/functions/conditions/condition-set";
import {Condition} from "../models/functions/conditions/condition";
import {ConditionSetOperator} from "../data/condition-set-operator";
import {ConditionOperatorMessage} from "../data/condition-operator";
import {isStringNotNullOrEmpty, stringOrDefault} from "./string-utils";
import Evaluators from "../evaluators";
import {Function as FunctionModel} from "../models/functions/function";

export function evaluateFunction(
    allElements: AnyElement[],
    func: FunctionModel | undefined | null,
    customerInput: CustomerInput,
    element: AnyElement,
    id: string,
    returnBoolean: boolean
): any {
    if (func == null) {
        return null;
    }

    if (isStringNotNullOrEmpty(func.code)) {
        return evaluateFunctionCode(func, customerInput, element, id);
    } else {
        const ret = evaluateFunctionNoCode(allElements, func, customerInput);
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
    allElements: AnyElement[],
    func: FunctionModel,
    customerInput: CustomerInput
): string | null {
    return func.conditionSet != null ? evaluateConditionSet(allElements, func.conditionSet, customerInput) : null;
}

function evaluateConditionSet(
    allElements: AnyElement[],
    conditionSet: ConditionSet,
    customerInput: CustomerInput
): string | null {
    switch (conditionSet.operator) {
        case ConditionSetOperator.All:
            if (conditionSet.conditions != null) {
                for (const cond of conditionSet.conditions) {
                    const res = evaluateCondition(allElements, cond, customerInput);
                    if (res != null) {
                        return stringOrDefault(conditionSet.conditionSetUnmetMessage, res);
                    }
                }
            }
            if (conditionSet.conditionsSets != null) {
                for (const condSet of conditionSet.conditionsSets) {
                    const res = evaluateConditionSet(allElements, condSet, customerInput);
                    if (res != null) {
                        return stringOrDefault(conditionSet.conditionSetUnmetMessage, res);
                    }
                }
            }
            return null;
        case ConditionSetOperator.Any:
            if (
                !(conditionSet.conditions ?? []).some(cond => evaluateCondition(allElements, cond, customerInput) == null) &&
                !(conditionSet.conditionsSets ?? []).some(condSet => evaluateConditionSet(allElements, condSet, customerInput) == null)
            ) {
                return stringOrDefault(conditionSet.conditionSetUnmetMessage, 'Keine der Bedingungen ist erfüllt');
            }
            return null;
    }
}

function evaluateCondition(allElements: AnyElement[], condition: Condition, customerInput: CustomerInput): string | null {
    if (condition.operator == null) {
        console.log('No operator', condition);
        return null;
    }

    const referencedElement = allElements.find(elem => elem.id === condition.reference);
    if (referencedElement == null) {
        console.log('No referencedElement', condition, allElements);
        return null;
    }

    const evaluator = Evaluators[referencedElement.type];
    if (evaluator == null) {
        console.log('No evaluator', condition, referencedElement);
        return null;
    }

    const operatorEvaluator = evaluator[condition.operator];
    if (operatorEvaluator == null) {
        console.log('No operatorEvaluator', condition, referencedElement);
        return null;
    }

    const referenceValue = customerInput[condition.reference];
    const targetValue = condition.target != null ? customerInput[condition.target] : condition.value ?? '';

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

import {CustomerInput} from "../models/customer-input";
import {AnyElement} from "../models/elements/any-element";
import {FunctionNoCode} from "../models/functions/function-no-code";
import {FunctionCode, isFunctionCode} from "../models/functions/function-code";
import {ConditionSet} from "../models/functions/conditions/condition-set";
import {Condition} from "../models/functions/conditions/condition";
import {ConditionSetOperator} from "../data/condition-set-operator";
import {isConditionOperandReference} from "../models/functions/conditions/condition-operand-reference";
import {ConditionOperator, ConditionOperatorLabel} from "../data/condition-operator";
import {stringOrDefault} from "./string-utils";

export function evaluateFunction(func: FunctionNoCode | FunctionCode | undefined | null, customerInput: CustomerInput, element: AnyElement, id: string, returnBoolean: boolean): any {
    if (func == null) {
        return null;
    }

    if (isFunctionCode(func)) {
        return evaluateFunctionCode(func, customerInput, element, id);
    } else {
        const ret = evaluateFunctionNoCode(func, customerInput);
        return returnBoolean ? ret == null : ret;
    }
}

function evaluateFunctionCode(func: FunctionCode, customerInput: CustomerInput, element: AnyElement, id: string): any {
    const fn = new Function('data', 'element', 'id', `
            ${func.code}
            return main(data, element, id);
        `);
    return fn(customerInput, element, id);
}

function evaluateFunctionNoCode(func: FunctionNoCode, customerInput: CustomerInput): string | null {
    return func.conditionSet != null ? evaluateConditionSet(func.conditionSet, customerInput) : null;
}

function evaluateConditionSet(conditionSet: ConditionSet, customerInput: CustomerInput): string | null {
    switch (conditionSet.operator) {
        case ConditionSetOperator.All:
            if (conditionSet.conditions != null) {
                for (const cond of conditionSet.conditions) {
                    const res = evaluateCondition(cond, customerInput);
                    if (res != null) {
                        return stringOrDefault(conditionSet.conditionSetUnmetMessage, res);
                    }
                }
            }
            if (conditionSet.conditionsSets != null) {
                for (const condSet of conditionSet.conditionsSets) {
                    const res = evaluateConditionSet(condSet, customerInput);
                    if (res != null) {
                        return stringOrDefault(conditionSet.conditionSetUnmetMessage, res);
                    }
                }
            }
            return null;
        case ConditionSetOperator.Any:
            if (
                !(conditionSet.conditions ?? []).some(cond => evaluateCondition(cond, customerInput) == null) &&
                !(conditionSet.conditionsSets ?? []).some(condSet => evaluateConditionSet(condSet, customerInput) == null)
            ) {
                return stringOrDefault(conditionSet.conditionSetUnmetMessage, 'Keine der Bedingungen ist erfüllt');
            }
            return null;
    }
}

function evaluateCondition(condition: Condition, customerInput: CustomerInput): string | null {
    let valueA = isConditionOperandReference(condition.operandA) ? customerInput[condition.operandA.id] : condition.operandA?.value;
    if (typeof valueA === 'boolean') {
        valueA = valueA ? 'Ja' : 'Nein';
    }

    let valueB = isConditionOperandReference(condition.operandB) ? customerInput[condition.operandB.id] : condition.operandB?.value;
    if (typeof valueB === 'boolean') {
        valueB = valueB ? 'Ja' : 'Nein';
    }

    /*
    console.log(
        valueA,
        condition.operator != null ? ConditionOperatorLabel[condition.operator] : 'No Operator',
        valueB,
    );
     */

    switch (condition.operator) {
        case ConditionOperator.Equals:
            if (valueA == valueB) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht ungleich "${valueB}" sein`);

        case ConditionOperator.EqualsIgnoreCase:
            if ((valueA == null && valueB == null) || (valueA.toLowerCase() === valueB.toLowerCase())) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht ungleich "${valueB}" sein`);

        case ConditionOperator.NotEquals:
            if (valueA != valueB) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht gleich "${valueB}" sein`);

        case ConditionOperator.NotEqualsIgnoreCase:
            if ((valueA == null && valueB == null) || (valueA.toLowerCase() !== valueB.toLowerCase())) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht gleich "${valueB}" sein`);

        case ConditionOperator.LessThan:
            if (valueA >= valueB) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht größer oder gleich "${valueB}" sein`);

        case ConditionOperator.LessThanOrEqual:
            if (valueA > valueB) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht größer "${valueB}" sein`);

        case ConditionOperator.GreaterThan:
            if (valueA <= valueB) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht kleiner oder gleich "${valueB}" sein`);

        case ConditionOperator.GreaterThanOrEqual:
            if (valueA < valueB) {
                return null;
            }
            return condMessage(condition, `"${valueA}" darf nicht kleiner "${valueB}" sein`);

        case ConditionOperator.Includes:
            if (typeof valueA === 'string' || Array.isArray(valueA)) {
                if (valueA.includes(valueB)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" beinhaltet "${valueB}" nicht`);
            }
            return null;

        case ConditionOperator.NotIncludes:
            if (typeof valueA === 'string' || Array.isArray(valueA)) {
                if (!valueA.includes(valueB)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" darf "${valueB}" nicht enthalten`);
            }
            return null;

        case ConditionOperator.StartsWith:
            if (typeof valueA === 'string') {
                if (valueA.startsWith(valueB)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" muss mit "${valueB}" beginnen`);
            }
            return null;

        case ConditionOperator.NotStartsWith:
            if (typeof valueA === 'string') {
                if (!valueA.startsWith(valueB)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" darf nicht mit "${valueB}" beginnen`);
            }
            return null;

        case ConditionOperator.EndsWith:
            if (typeof valueA === 'string') {
                if (valueA.endsWith(valueB)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" muss mit "${valueB}" enden`);
            }
            return null;

        case ConditionOperator.NotEndsWith:
            if (typeof valueA === 'string') {
                if (!valueA.endsWith(valueB)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" darf nicht mit "${valueB}" enden`);
            }
            return null;

        case ConditionOperator.MatchesPattern:
            if (typeof valueA === 'string' && typeof valueB === 'string') {
                const re = new RegExp(`^${valueB}$`);
                if (re.test(valueA)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" muss dem Muster "${valueB}" entsprechen`);
            }
            return null;

        case ConditionOperator.NotMatchesPattern:
            if (typeof valueA === 'string' && typeof valueB === 'string') {
                const re = new RegExp(`^${valueB}$`);
                if (!re.test(valueA)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" darf nicht dem Muster "${valueB}" entsprechen`);
            }
            return null;

        case ConditionOperator.IncludesPattern:
            if (typeof valueA === 'string' && typeof valueB === 'string') {
                const re = new RegExp(`${valueB}`);
                if (re.test(valueA)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" muss dem Muster "${valueB}" enthalten`);
            }
            return null;

        case ConditionOperator.NotIncludesPattern:
            if (typeof valueA === 'string' && typeof valueB === 'string') {
                const re = new RegExp(`^${valueB}$`);
                if (!re.test(valueA)) {
                    return null;
                }
                return condMessage(condition, `"${valueA}" darf nicht das Muster "${valueB}" enthalten`);
            }
            return null;
    }

    return null;
}

function condMessage(condition: Condition, message: string) {
    return stringOrDefault(condition.conditionUnmetMessage, message);
}

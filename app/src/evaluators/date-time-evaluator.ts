import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../utils/string-utils';
import {compareInstantIso} from '../utils/temporal-utils';

function compareValues(left: unknown, right: unknown): number | null {
    return compareInstantIso(left, right);
}

export const DateTimeEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        const comparison = compareValues(valueA, valueB);
        return comparison != null && comparison === 0;
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        const comparison = compareValues(valueA, valueB);
        return comparison != null && comparison !== 0;
    },

    [ConditionOperator.LessThan]: (valueA, valueB) => {
        const comparison = compareValues(valueA, valueB);
        return comparison != null && comparison < 0;
    },
    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) => {
        const comparison = compareValues(valueA, valueB);
        return comparison != null && comparison <= 0;
    },

    [ConditionOperator.GreaterThan]: (valueA, valueB) => {
        const comparison = compareValues(valueA, valueB);
        return comparison != null && comparison > 0;
    },
    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) => {
        const comparison = compareValues(valueA, valueB);
        return comparison != null && comparison >= 0;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return isStringNullOrEmpty(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return isStringNotNullOrEmpty(valueA);
    },
};

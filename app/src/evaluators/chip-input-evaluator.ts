import {ConditionOperator} from '../data/condition-operator';
import {type BaseEvaluator} from './base-evaluator';

export const ChipInputEvaluator: BaseEvaluator<string[]> = {
    [ConditionOperator.Includes]: (valueA, valueB) => {
        const entries = valueA ?? [];

        if (Array.isArray(valueB)) {
            return valueB.every(vb => entries.includes(vb));
        } else if (typeof valueB === 'string') {
            return entries.includes(valueB);
        }

        return false;
    },
    [ConditionOperator.NotIncludes]: (valueA, valueB) => {
        const entries = valueA ?? [];

        if (Array.isArray(valueB)) {
            return !valueB.some(vb => entries.includes(vb));
        } else if (typeof valueB === 'string') {
            return !entries.includes(valueB);
        }

        return false;
    },
    [ConditionOperator.Empty]: (valueA) => {
        return valueA == null || valueA.length === 0;
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return valueA != null && valueA.length > 0;
    },
};

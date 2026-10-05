import {ConditionOperator} from '../data/condition-operator';
import {type BaseEvaluator} from './base-evaluator';

export const ChipInputEvaluator: BaseEvaluator<string[]> = {
    [ConditionOperator.Includes]: (valueA, valueB) => {
        if (valueA == null) {
            return valueB == null;
        }

        if (Array.isArray(valueB)) {
            return valueB.every(vb => valueA.includes(vb));
        } else if (typeof valueB === 'string') {
            return valueA.includes(valueB);
        }

        return false;
    },
    [ConditionOperator.NotIncludes]: (valueA, valueB) => {
        if (valueA == null) {
            return valueB != null;
        }

        if (Array.isArray(valueB)) {
            return !valueB.every(vb => valueA.includes(vb));
        } else if (typeof valueB === 'string') {
            return !valueA.includes(valueB);
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

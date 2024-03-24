import {ConditionOperator} from '../data/condition-operator';
import {type BaseEvaluator} from './base-evaluator';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../utils/string-utils';


export const TextEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return valueA === valueB;
    },
    [ConditionOperator.EqualsIgnoreCase]: (valueA, valueB) => {
        return (valueA === valueB) || (valueA != null && typeof valueB === 'string' && valueA.toLowerCase() === valueB.toLowerCase());
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return valueA !== valueB;
    },
    [ConditionOperator.NotEqualsIgnoreCase]: (valueA, valueB) => {
        return !((valueA === valueB) || (valueA != null && typeof valueB === 'string' && valueA.toLowerCase() === valueB.toLowerCase()));
    },

    [ConditionOperator.Includes]: (valueA, valueB) => {
        return valueA != null && valueA.includes(valueB);
    },
    [ConditionOperator.NotIncludes]: (valueA, valueB) => {
        return valueA == null || !valueA.includes(valueB);
    },

    [ConditionOperator.StartsWith]: (valueA, valueB) => {
        return valueA != null && valueA.startsWith(valueB);
    },
    [ConditionOperator.NotStartsWith]: (valueA, valueB) => {
        return valueA == null || !valueA.startsWith(valueB);
    },

    [ConditionOperator.EndsWith]: (valueA, valueB) => {
        return valueA != null && valueA.endsWith(valueB);
    },
    [ConditionOperator.NotEndsWith]: (valueA, valueB) => {
        return valueA == null || !valueA.endsWith(valueB);
    },

    [ConditionOperator.MatchesPattern]: (valueA, valueB) => {
        if (valueA == null) {
            return false;
        }
        if (typeof valueB !== 'string') {
            return false;
        }
        const re = new RegExp(`^${valueB}$`);
        return re.test(valueA);
    },
    [ConditionOperator.NotMatchesPattern]: (valueA, valueB) => {
        if (valueA == null) {
            return false;
        }
        if (typeof valueB !== 'string') {
            return false;
        }
        const re = new RegExp(`^${valueB}$`);
        return !re.test(valueA);
    },

    [ConditionOperator.IncludesPattern]: (valueA, valueB) => {
        if (valueA == null) {
            return false;
        }
        if (typeof valueB !== 'string') {
            return false;
        }
        const re = new RegExp(valueB);
        return re.test(valueA);
    },
    [ConditionOperator.NotIncludesPattern]: (valueA, valueB) => {
        if (valueA == null) {
            return false;
        }
        if (typeof valueB !== 'string') {
            return false;
        }
        const re = new RegExp(valueB);
        return !re.test(valueA);
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return isStringNullOrEmpty(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return isStringNotNullOrEmpty(valueA);
    },
};

import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../utils/string-utils';
import {isLocalTimeIso} from '../utils/temporal-utils';

function transformValue(val: any): number | null {
    if (val == null) {
        return null;
    }
    if (typeof val !== 'string') {
        return null;
    }

    if (!isLocalTimeIso(val)) {
        return null;
    }

    const [hour, minute, second = 0] = val.split(':').map(Number);
    return hour * 3600 + minute * 60 + second;
}

export const TimeEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA === tValB;
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA !== tValB;
    },

    [ConditionOperator.LessThan]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA < tValB;
    },
    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA <= tValB;
    },

    [ConditionOperator.GreaterThan]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA > tValB;
    },
    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA >= tValB;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return isStringNullOrEmpty(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return isStringNotNullOrEmpty(valueA);
    },
};

import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../utils/string-utils';
import {isValid, parse, parseISO} from 'date-fns';

function transformValue(val: any): number | null {
    if (val == null || typeof val !== 'string') {
        return null;
    }

    const isoDate = parseISO(val);
    if (isValid(isoDate)) {
        return isoDate.getTime();
    }

    const germanDateTime = parse(val, 'dd.MM.yyyy HH:mm', new Date());
    if (isValid(germanDateTime)) {
        return germanDateTime.getTime();
    }

    const germanDate = parse(val, 'dd.MM.yyyy', new Date());
    if (isValid(germanDate)) {
        return germanDate.getTime();
    }

    return null;
}

export const DateTimeEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return transformValue(valueA) === transformValue(valueB);
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return transformValue(valueA) !== transformValue(valueB);
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

import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";

const defNumberRegex = /^[1-9][0-9]*?(\.[0-9]+)$/;

function transformValue(val: any): number | null {
    if (val == null) {
        return null;
    }

    switch (typeof val) {
        case 'number':
            return val;
        case 'string':
            if (val.match(defNumberRegex)) {
                return parseFloat(val);
            } else {
                return parseFloat(val.replace('.', '').replace(',', '.'));
            }
        default:
            return null;
    }
}

export const NumberEvaluator: BaseEvaluator<number> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return valueA === transformValue(valueB);
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return valueA !== transformValue(valueB);
    },

    [ConditionOperator.LessThan]: (valueA, valueB) => {
        const tValB = transformValue(valueB);
        return valueA != null && tValB != null && valueA < tValB;
    },
    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) => {
        const tValB = transformValue(valueB);
        return valueA != null && tValB != null && valueA <= tValB;
    },

    [ConditionOperator.GreaterThan]: (valueA, valueB) => {
        const tValB = transformValue(valueB);
        return valueA != null && tValB != null && valueA > tValB;
    },
    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) => {
        const tValB = transformValue(valueB);
        return valueA != null && tValB != null && valueA >= tValB;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null || isNaN(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null && !isNaN(valueA);
    },
};

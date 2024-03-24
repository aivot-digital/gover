import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";


export const MultiCheckboxEvaluator: BaseEvaluator<string[]> = {
    [ConditionOperator.Includes]: (valueA, valueB) => {
        if (valueA == null) {
            return valueB == null;
        }
        if (Array.isArray(valueB)) {
            return valueB.every(vb => valueA.includes(vb));
        } else if (typeof valueB === 'string') {
            return valueA.includes(valueB);
        } else {
            return false;
        }
    },
    [ConditionOperator.NotIncludes]: (valueA, valueB) => {
        if (valueA == null) {
            return valueB != null;
        }
        if (Array.isArray(valueB)) {
            return !valueB.every(vb => valueA.includes(vb));
        } else if (typeof valueB === 'string') {
            return !valueA.includes(valueB);
        } else {
            return false;
        }
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null || valueA.length === 0;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null && valueA.length > 0;
    },
};

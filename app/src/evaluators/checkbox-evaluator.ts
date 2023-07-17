import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";

const falseValue = 'Nein (False)';
const trueValue = 'Ja (True)';

export const CheckboxEvaluator: BaseEvaluator<boolean> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        if (valueA == null && valueB == null) {
            return true;
        }
        if (valueB === falseValue && (valueA == null || !valueA)) {
            return true;
        }
        return valueB === trueValue && valueA != null && valueA;

    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        if (valueA == null && valueB == null) {
            return false;
        }
        if (valueB === falseValue && (valueA == null || !valueA)) {
            return false;
        }
        return !(valueB === trueValue && valueA != null && valueA);
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null || !valueA;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null && valueA;
    },
};

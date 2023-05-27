import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";

function transformValue(val: any) {
    if (typeof val === 'boolean') {
        return val ? 'Ja (True)' : 'Nein (False)';
    }
    return val;
}

export const CheckboxEvaluator: BaseEvaluator<boolean> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return valueA == valueB || transformValue(valueA) === valueB;
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return valueA != valueB || transformValue(valueA) !== valueB;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null || !valueA;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null && valueA;
    },
};

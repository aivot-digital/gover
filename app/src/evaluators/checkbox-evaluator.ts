import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";

function transformValue(val: any) {
    return typeof val === 'boolean' && val ? 'Ja' : 'Nein';
}

export const CheckboxEvaluator: BaseEvaluator<boolean> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return valueA == valueB || transformValue(valueA) === valueB;
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return valueA != valueB || transformValue(valueA) !== valueB;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null;
    },
};

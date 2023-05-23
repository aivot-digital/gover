import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";

function transformValue(val: any) {
    return typeof val === 'boolean' && val ? 'Ja' : 'Nein';
}

export const CheckboxEvaluator: BaseEvaluator = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return  transformValue(valueA) === transformValue(valueB);
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return  transformValue(valueA) !== transformValue(valueB);
    },
};

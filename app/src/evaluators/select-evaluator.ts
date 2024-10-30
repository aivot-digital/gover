import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from "../utils/string-utils";


export const SelectEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return valueA === valueB;
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return valueA !== valueB;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return isStringNullOrEmpty(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return isStringNotNullOrEmpty(valueA);
    },
};

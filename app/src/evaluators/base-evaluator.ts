import {ConditionOperator} from "../data/condition-operator";

export type BaseEvaluator = {
    [op in ConditionOperator]?: (valueA: any, valueB: any) => boolean;
}

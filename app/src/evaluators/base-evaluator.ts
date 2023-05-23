import {ConditionOperator} from "../data/condition-operator";

export type BaseEvaluator<T> = {
    [op in ConditionOperator]?: (valueA: T | undefined, valueB: any) => boolean;
}

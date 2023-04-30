import {Condition} from "./condition";
import {ConditionSetOperator} from "../../../data/condition-set-operator";

export interface ConditionSet {
    operator: ConditionSetOperator;
    conditions?: Condition[];
    conditionsSets?: ConditionSet[];
    conditionSetUnmetMessage?: string;
}

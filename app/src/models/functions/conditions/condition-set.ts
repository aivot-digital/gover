import {Condition} from "./condition";

export interface ConditionSet {
    operator: number; // TODO: Enum
    conditions: Condition[];
    conditionsSets: ConditionSet[];
    conditionSetUnmetMessage: string;
}

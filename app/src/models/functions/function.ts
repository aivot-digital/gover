import {ConditionSet} from "./conditions/condition-set";

export interface Function {
    requirements: string;
    code?: string;
    conditionSet?: ConditionSet;
}

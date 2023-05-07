import {Function} from "./function";
import {ConditionSet} from "./conditions/condition-set";

export interface FunctionNoCode extends Function {
    conditionSet?: ConditionSet;
}

export function isFunctionNoCode(obj: any): obj is FunctionNoCode {
    return obj != null && obj.conditionSet != null;
}

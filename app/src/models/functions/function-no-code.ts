import {Function} from "./function";
import {ConditionSet} from "./conditions/condition-set";

export interface FunctionNoCode extends Function {
    conditionSet?: ConditionSet;
}

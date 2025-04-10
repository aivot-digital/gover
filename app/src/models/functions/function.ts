import {ConditionSet} from "./conditions/condition-set";

export interface Function {
    requirements: string;
    code?: string;
    conditionSet?: ConditionSet;
}

export function isCodeFunction(value: any): value is Function {
    return value != null && typeof value.requirements === 'string' && typeof value.code === 'string';
}

export function isNoCodeFunction(value: any): value is Function {
    return value != null && typeof value.requirements === 'string' && value.conditionSet != null;
}

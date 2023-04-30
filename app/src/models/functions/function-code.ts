import {Function} from "./function";

export interface FunctionCode extends Function {
    code: string;
}

export function isFunctionCode(obj: any): obj is FunctionCode {
    return obj != null && obj.code != null;
}

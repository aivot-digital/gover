import {Function} from "./function";

export interface FunctionCode extends Function {
    functions: {
        [key: string]: string;
    };
    mainFunction: string;
}

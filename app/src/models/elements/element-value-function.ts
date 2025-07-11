import {NoCodeExpression} from '../functions/no-code-expression';
import {JavascriptCode} from '../functions/javascript-code';

export interface ElementValueFunction {
    requirements: string | null | undefined;
    expression: NoCodeExpression | null | undefined;
    javascriptCode: JavascriptCode | null | undefined;
    referencedIds: string[] | null | undefined;
}
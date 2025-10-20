import {NoCodeOperand} from '../functions/no-code-expression';
import {JavascriptCode} from '../functions/javascript-code';

export interface ElementOverrideFunction {
    type: 'NoCode' | 'Javascript' | null | undefined;
    requirements: string | null | undefined;
    fieldNoCodeMap: Record<string, NoCodeOperand> | null | undefined;
    javascriptCode: JavascriptCode | null | undefined;
    referencedIds: string[] | null | undefined;
}
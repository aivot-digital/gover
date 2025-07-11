import {NoCodeExpression} from '../functions/no-code-expression';
import {JavascriptCode} from '../functions/javascript-code';
import {ConditionSet} from '../functions/conditions/condition-set';

export interface ElementVisibilityFunction {
    requirements: string | null | undefined;
    conditionSet: ConditionSet | null | undefined;
    expression: NoCodeExpression | null | undefined;
    javascriptCode: JavascriptCode | null | undefined;
    referencedIds: string[] | null | undefined;
}
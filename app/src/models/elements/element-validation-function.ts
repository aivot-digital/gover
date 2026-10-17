import {ValidationExpressionWrapper} from '../functions/no-code-expression';
import {JavascriptCode} from '../functions/javascript-code';
import {ConditionSet} from '../functions/conditions/condition-set';

export interface ElementValidationFunction {
    type: 'NoCode' | 'ConditionSet' | 'Javascript' | null | undefined;
    requirements: string | null | undefined;
    conditionSet: ConditionSet | null | undefined;
    noCodeList: ValidationExpressionWrapper[] | null | undefined;
    javascriptCode: JavascriptCode | null | undefined;
    referencedIds: string[] | null | undefined;
}
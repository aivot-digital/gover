import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {HtmlTemplateInputValue} from '../models/elements/form/input/html-template-input-element';
import {isStringNotNullOrEmpty} from '../utils/string-utils';

function isFilled(value: HtmlTemplateInputValue | null | undefined): boolean {
    return isStringNotNullOrEmpty(value?.assetKey);
}

export const HtmlTemplateInputEvaluator: BaseEvaluator<HtmlTemplateInputValue> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

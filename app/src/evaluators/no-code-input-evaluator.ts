import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {NoCodeInputFieldElementItem} from '../models/elements/form/input/no-code-input-field-element';

function isFilled(value: NoCodeInputFieldElementItem | null | undefined): boolean {
    return value?.noCode != null;
}

export const NoCodeInputEvaluator: BaseEvaluator<NoCodeInputFieldElementItem> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

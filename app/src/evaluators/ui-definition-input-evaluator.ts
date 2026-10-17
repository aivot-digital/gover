import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {UiDefinitionInputFieldElementItem} from '../models/elements/form/input/ui-definition-input-field-element';

function isFilled(value: UiDefinitionInputFieldElementItem | null | undefined): boolean {
    return value != null;
}

export const UiDefinitionInputEvaluator: BaseEvaluator<UiDefinitionInputFieldElementItem> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

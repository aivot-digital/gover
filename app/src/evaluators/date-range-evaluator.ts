import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {DateRangeValue} from '../models/elements/form/input/date-range-field-element';

function isFilled(value: DateRangeValue | null | undefined): boolean {
    return (value?.start != null && value.start.length > 0) || (value?.end != null && value.end.length > 0);
}

export const DateRangeEvaluator: BaseEvaluator<DateRangeValue> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

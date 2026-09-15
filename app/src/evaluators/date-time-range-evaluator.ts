import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {DateTimeRangeValue} from '../models/elements/form/input/date-time-range-field-element';

function isFilled(value: DateTimeRangeValue | null | undefined): boolean {
    return (value?.start != null && value.start.length > 0) || (value?.end != null && value.end.length > 0);
}

export const DateTimeRangeEvaluator: BaseEvaluator<DateTimeRangeValue> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

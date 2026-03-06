import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {TimeRangeValue} from '../models/elements/form/input/time-range-field-element';

function isFilled(value: TimeRangeValue | null | undefined): boolean {
    return (value?.start != null && value.start.length > 0) || (value?.end != null && value.end.length > 0);
}

export const TimeRangeEvaluator: BaseEvaluator<TimeRangeValue> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

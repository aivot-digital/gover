import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {MapPointValue} from '../models/elements/form/input/map-point-field-element';

function isFilled(value: MapPointValue | null | undefined): boolean {
    const hasCoordinates = value?.latitude != null && value?.longitude != null;
    const hasAddress = value?.address != null && value.address.length > 0;
    return hasCoordinates || hasAddress;
}

export const MapPointEvaluator: BaseEvaluator<MapPointValue> = {
    [ConditionOperator.Empty]: (valueA) => {
        return !isFilled(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return isFilled(valueA);
    },
};

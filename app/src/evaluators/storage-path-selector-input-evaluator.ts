import {ConditionOperator} from '../data/condition-operator';
import {type BaseEvaluator} from './base-evaluator';
import {type StoragePathSelectorInputElementValue} from '../models/elements/form/input/storage-path-selector-input-element';
import {isStringNotNullOrEmpty} from '../utils/string-utils';

function normalizePath(path: string | null | undefined): string | null {
    if (path == null || path.trim().length === 0) {
        return null;
    }

    return path.trim();
}

function isFilled(value: StoragePathSelectorInputElementValue | null | undefined): boolean {
    return value?.storageProviderId != null && isStringNotNullOrEmpty(value.path);
}

function equalsStoragePath(valueA: StoragePathSelectorInputElementValue | undefined, valueB: any): boolean {
    if (!isFilled(valueA)) {
        return false;
    }

    if (valueB != null && typeof valueB === 'object') {
        return valueA?.storageProviderId === valueB.storageProviderId && normalizePath(valueA?.path) === normalizePath(valueB.path);
    }

    if (typeof valueB === 'string') {
        return normalizePath(valueA?.path) === normalizePath(valueB);
    }

    return false;
}

export const StoragePathSelectorInputEvaluator: BaseEvaluator<StoragePathSelectorInputElementValue> = {
    [ConditionOperator.Equals]: equalsStoragePath,
    [ConditionOperator.NotEquals]: (valueA, valueB) => !equalsStoragePath(valueA, valueB),
    [ConditionOperator.Empty]: (valueA) => !isFilled(valueA),
    [ConditionOperator.NotEmpty]: (valueA) => isFilled(valueA),
};

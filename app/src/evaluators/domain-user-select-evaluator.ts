import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {
    createDomainAndUserSelectValueKey,
    normalizeDomainAndUserSelectItem,
} from '../components/domain-user-select-field/domain-user-select-options';
import {DomainAndUserSelectItem} from '../models/elements/form/input/domain-user-select-field-element';

function normalizeList(value: unknown): DomainAndUserSelectItem[] {
    if (!Array.isArray(value)) {
        return [];
    }

    return value
        .map((entry) => normalizeDomainAndUserSelectItem(entry))
        .filter((entry): entry is DomainAndUserSelectItem => entry != null);
}

function normalizeComparedKeys(value: unknown): string[] {
    if (Array.isArray(value)) {
        return value
            .map((entry) => normalizeDomainAndUserSelectItem(entry))
            .filter((entry): entry is DomainAndUserSelectItem => entry != null)
            .map((entry) => createDomainAndUserSelectValueKey(entry));
    }

    const normalizedSingleValue = normalizeDomainAndUserSelectItem(value);
    if (normalizedSingleValue != null) {
        return [createDomainAndUserSelectValueKey(normalizedSingleValue)];
    }

    return [];
}

export const DomainUserSelectEvaluator: BaseEvaluator<DomainAndUserSelectItem[]> = {
    [ConditionOperator.Includes]: (valueA, valueB) => {
        const normalizedValueA = normalizeList(valueA);
        const comparedKeys = normalizeComparedKeys(valueB);

        if (comparedKeys.length === 0) {
            return false;
        }

        const currentKeys = new Set(normalizedValueA.map((entry) => createDomainAndUserSelectValueKey(entry)));
        return comparedKeys.every((key) => currentKeys.has(key));
    },
    [ConditionOperator.NotIncludes]: (valueA, valueB) => {
        const normalizedValueA = normalizeList(valueA);
        const comparedKeys = normalizeComparedKeys(valueB);

        if (comparedKeys.length === 0) {
            return true;
        }

        const currentKeys = new Set(normalizedValueA.map((entry) => createDomainAndUserSelectValueKey(entry)));
        return comparedKeys.every((key) => !currentKeys.has(key));
    },
    [ConditionOperator.Empty]: (valueA) => {
        return normalizeList(valueA).length === 0;
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return normalizeList(valueA).length > 0;
    },
};

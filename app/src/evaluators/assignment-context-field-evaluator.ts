import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {
    createDomainAndUserSelectValueKey,
    normalizeDomainAndUserSelectItem,
} from '../components/domain-user-select-field/domain-user-select-options';
import {AssignmentContextValue} from '../models/elements/form/input/assignment-context-field-element';
import {DomainAndUserSelectItem} from '../models/elements/form/input/domain-user-select-field-element';

function normalizeGeneralAssigneePreference(value: unknown): AssignmentContextValue['generalAssigneePreference'] {
    switch (value) {
        case 'previousProcessStepAssignee':
        case 'uninvolvedUser':
        case 'processInstanceAssignee':
            return value;
        default:
            return undefined;
    }
}

function normalizeRepeatExecutionAssigneePreference(value: unknown): AssignmentContextValue['repeatExecutionAssigneePreference'] {
    switch (value) {
        case 'previousIterationAssignee':
        case 'differentFromPreviousIterationAssignee':
            return value;
        default:
            return undefined;
    }
}

function normalizeDomainSelection(value: unknown): DomainAndUserSelectItem[] {
    if (!Array.isArray(value)) {
        return [];
    }

    const values = value
        .map((entry) => normalizeDomainAndUserSelectItem(entry))
        .filter((entry): entry is DomainAndUserSelectItem => entry != null);

    const uniqueValues = new Map<string, DomainAndUserSelectItem>();
    values.forEach((entry) => {
        uniqueValues.set(createDomainAndUserSelectValueKey(entry), entry);
    });

    return Array.from(uniqueValues.values());
}

function normalizeAssignmentContextValue(value: unknown): AssignmentContextValue | undefined {
    if (value == null) {
        return undefined;
    }

    if (Array.isArray(value)) {
        const normalizedSelection = normalizeDomainSelection(value);
        return normalizedSelection.length === 0
            ? undefined
            : {
                domainAndUserSelection: normalizedSelection,
                generalAssigneePreference: undefined,
                repeatExecutionAssigneePreference: undefined,
            };
    }

    if (typeof value !== 'object') {
        return undefined;
    }

    const rawValue = value as Record<string, unknown>;
    const normalizedSelection = normalizeDomainSelection(rawValue.domainAndUserSelection);
    const generalAssigneePreference = normalizeGeneralAssigneePreference(rawValue.generalAssigneePreference);
    const repeatExecutionAssigneePreference = normalizeRepeatExecutionAssigneePreference(rawValue.repeatExecutionAssigneePreference);

    if (
        normalizedSelection.length === 0 &&
        generalAssigneePreference == null &&
        repeatExecutionAssigneePreference == null
    ) {
        return undefined;
    }

    return {
        domainAndUserSelection: normalizedSelection,
        generalAssigneePreference,
        repeatExecutionAssigneePreference,
    };
}

function normalizeComparedKeys(value: unknown): string[] {
    if (Array.isArray(value)) {
        return value
            .map((entry) => normalizeDomainAndUserSelectItem(entry))
            .filter((entry): entry is DomainAndUserSelectItem => entry != null)
            .map((entry) => createDomainAndUserSelectValueKey(entry));
    }

    if (value != null && typeof value === 'object') {
        const normalizedValue = normalizeAssignmentContextValue(value);
        if (normalizedValue?.domainAndUserSelection != null) {
            return normalizedValue.domainAndUserSelection
                .map((entry) => createDomainAndUserSelectValueKey(entry));
        }
    }

    const normalizedSingleValue = normalizeDomainAndUserSelectItem(value);
    if (normalizedSingleValue != null) {
        return [createDomainAndUserSelectValueKey(normalizedSingleValue)];
    }

    return [];
}

export const AssignmentContextFieldEvaluator: BaseEvaluator<AssignmentContextValue> = {
    [ConditionOperator.Includes]: (valueA, valueB) => {
        const normalizedValueA = normalizeAssignmentContextValue(valueA);
        const comparedKeys = normalizeComparedKeys(valueB);

        if (normalizedValueA == null || comparedKeys.length === 0) {
            return false;
        }

        const currentKeys = new Set(
            (normalizedValueA.domainAndUserSelection ?? [])
                .map((entry) => createDomainAndUserSelectValueKey(entry)),
        );

        return comparedKeys.every((key) => currentKeys.has(key));
    },
    [ConditionOperator.NotIncludes]: (valueA, valueB) => {
        const normalizedValueA = normalizeAssignmentContextValue(valueA);
        const comparedKeys = normalizeComparedKeys(valueB);

        if (comparedKeys.length === 0) {
            return true;
        }

        const currentKeys = new Set(
            (normalizedValueA?.domainAndUserSelection ?? [])
                .map((entry) => createDomainAndUserSelectValueKey(entry)),
        );

        return comparedKeys.every((key) => !currentKeys.has(key));
    },
    [ConditionOperator.Empty]: (valueA) => {
        return normalizeAssignmentContextValue(valueA) == null;
    },
    [ConditionOperator.NotEmpty]: (valueA) => {
        return normalizeAssignmentContextValue(valueA) != null;
    },
};

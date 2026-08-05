import type {
    GeneralAssigneePreference,
    RepeatExecutionAssigneePreference,
} from '../models/elements/form/input/assignment-context-field-element';

type AssignmentContextPreferenceOption<T extends string> = {
    value: T;
    label: string;
    subLabel: string;
};

export const assignmentContextGeneralAssigneePreferenceOptions: Array<AssignmentContextPreferenceOption<GeneralAssigneePreference>> = [
    {
        value: 'none',
        label: 'Keine Bevorzugung',
        subLabel: 'Es greifen keine zusätzlichen Präferenzen; bei mehreren Personen entscheidet die Aufgabenlast.',
    },
    {
        value: 'previousProcessStepAssignee',
        label: 'Bevorzuge Bearbeiter:in des vorherigen Prozessschritts',
        subLabel: 'Nutzt die Person aus dem vorherigen Prozessschritt, sofern sie weiterhin berechtigt ist.',
    },
    {
        value: 'uninvolvedUser',
        label: 'Bevorzuge eine neue, unbeteiligte Mitarbeiter:in',
        subLabel: 'Wählt möglichst eine Person, die in diesem Vorgang noch nicht beteiligt war.',
    },
    {
        value: 'processInstanceAssignee',
        label: 'Bevorzuge die dem Vorgang zugewiesene Mitarbeiter:in',
        subLabel: 'Nutzt die zentrale Vorgangszuweisung, sofern diese Person zum Personenkreis gehört.',
    },
];

export const assignmentContextRepeatExecutionAssigneePreferenceOptions: Array<AssignmentContextPreferenceOption<RepeatExecutionAssigneePreference>> = [
    {
        value: 'none',
        label: 'Keine Bevorzugung',
        subLabel: 'Die Aufgabe wird regulär nach Personenkreis und allgemeiner Bevorzugung zugewiesen.',
    },
    {
        value: 'previousIterationAssignee',
        label: 'Bevorzuge zuletzt zuständige Mitarbeiter:in',
        subLabel: 'Nutzt die Person aus der vorherigen Ausführung dieser Aufgabe, sofern sie weiterhin berechtigt ist.',
    },
    {
        value: 'differentFromPreviousIterationAssignee',
        label: 'Bevorzuge eine andere Mitarbeiter:in',
        subLabel: 'Vermeidet nach Möglichkeit die Person aus der vorherigen Ausführung dieser Aufgabe.',
    },
];

export function getAssignmentContextGeneralAssigneePreferenceLabel(value: string | null | undefined): string | undefined {
    if (value == null || value === 'none') {
        return undefined;
    }

    return assignmentContextGeneralAssigneePreferenceOptions
        .find((option) => option.value === value)
        ?.label;
}

export function getAssignmentContextRepeatExecutionAssigneePreferenceLabel(value: string | null | undefined): string | undefined {
    if (value == null || value === 'none') {
        return undefined;
    }

    return assignmentContextRepeatExecutionAssigneePreferenceOptions
        .find((option) => option.value === value)
        ?.label;
}

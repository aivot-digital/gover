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
        label: 'Zuständige Person des vorherigen Prozessschritts bevorzugen',
        subLabel: 'Bevorzugt die zuständige Person des vorherigen Prozessschritts, sofern sie zum Personenkreis gehört und die erforderlichen Berechtigungen besitzt.',
    },
    {
        value: 'uninvolvedUser',
        label: 'Bisher unbeteiligte Mitarbeiter:in bevorzugen',
        subLabel: 'Bevorzugt eine Person, der in diesem Vorgang bisher keine Aufgabe zugewiesen wurde.',
    },
    {
        value: 'processInstanceAssignee',
        label: 'Zuständige Person des Vorgangs bevorzugen',
        subLabel: 'Bevorzugt die zuständige Person des Vorgangs, sofern sie zum Personenkreis gehört und die erforderlichen Berechtigungen besitzt.',
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
        label: 'Zuletzt zuständige Mitarbeiter:in bevorzugen',
        subLabel: 'Bevorzugt die zuständige Person der vorherigen Ausführung dieses Prozesselements, sofern sie zum Personenkreis gehört und die erforderlichen Berechtigungen besitzt.',
    },
    {
        value: 'differentFromPreviousIterationAssignee',
        label: 'Andere Mitarbeiter:in bevorzugen',
        subLabel: 'Vermeidet nach Möglichkeit die zuständige Person der vorherigen Ausführung dieses Prozesselements.',
    },
];

export function getAvailableAssignmentContextGeneralAssigneePreferenceOptions(disableProcessInstanceAssigneeOption: boolean) {
    return disableProcessInstanceAssigneeOption
        ? assignmentContextGeneralAssigneePreferenceOptions.filter((option) => option.value !== 'processInstanceAssignee')
        : assignmentContextGeneralAssigneePreferenceOptions;
}

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

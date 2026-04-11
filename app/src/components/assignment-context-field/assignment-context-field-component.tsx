import {Stack, Typography} from '@mui/material';
import {RadioFieldComponent} from '../radio-field/radio-field-component';
import {DomainUserSelectFieldComponent} from '../domain-user-select-field/domain-user-select-field-component';
import {
    AssignmentContextValue,
} from '../../models/elements/form/input/assignment-context-field-element';
import {DomainAndUserSelectOption} from '../domain-user-select-field/domain-user-select-options';
import {
    DomainAndUserSelectItemType,
    DomainAndUserSelectProcessAccessConstraint,
} from '../../models/elements/form/input/domain-user-select-field-element';

export interface AssignmentContextFieldComponentProps {
    value?: AssignmentContextValue | null;
    onChange: (value: AssignmentContextValue | undefined) => void;

    title?: string;
    description?: string;

    domainAndUserSelectionLabel?: string;
    domainAndUserSelectionHint?: string;
    domainAndUserSelectionPlaceholder?: string;
    domainAndUserSelectionError?: string;

    preferredPreviousTaskAssigneeLabel?: string;
    preferredUninvolvedUserLabel?: string;
    preferredProcessInstanceAssigneeLabel?: string;

    disabled?: boolean;
    readOnly?: boolean;
    required?: boolean;

    options?: DomainAndUserSelectOption[];
    allowedTypes?: DomainAndUserSelectItemType[] | null;
    processAccessConstraint?: DomainAndUserSelectProcessAccessConstraint | null;
}

type PreferredAssigneeOption = 'none' | 'previous' | 'uninvolved' | 'processInstance';
const DEFAULT_HEADLINE = 'Verantwortlicher Personenkreis';
const DEFAULT_TEXT = 'Definieren Sie den Personenkreis, der für diese Aufgabe herangezogen werden kann.';

function resolvePreferredAssigneeOption(value: AssignmentContextValue): PreferredAssigneeOption {
    if (value.preferPreviousTaskAssignee === true) {
        return 'previous';
    }

    if (value.preferUninvolvedUser === true) {
        return 'uninvolved';
    }

    if (value.preferProcessInstanceAssignee === true) {
        return 'processInstance';
    }

    return 'none';
}

function normalizeValue(value: AssignmentContextValue): AssignmentContextValue | undefined {
    const domainAndUserSelection = value.domainAndUserSelection != null && value.domainAndUserSelection.length > 0
        ? value.domainAndUserSelection
        : undefined;

    const selectedPreference = resolvePreferredAssigneeOption(value);
    const preferPreviousTaskAssignee = selectedPreference === 'previous';
    const preferUninvolvedUser = selectedPreference === 'uninvolved';
    const preferProcessInstanceAssignee = selectedPreference === 'processInstance';

    if (domainAndUserSelection == null && !preferPreviousTaskAssignee && !preferUninvolvedUser && !preferProcessInstanceAssignee) {
        return undefined;
    }

    return {
        domainAndUserSelection,
        preferPreviousTaskAssignee,
        preferUninvolvedUser,
        preferProcessInstanceAssignee,
    };
}

export function AssignmentContextFieldComponent(props: AssignmentContextFieldComponentProps) {
    const currentValue: AssignmentContextValue = {
        domainAndUserSelection: props.value?.domainAndUserSelection,
        preferPreviousTaskAssignee: props.value?.preferPreviousTaskAssignee === true,
        preferUninvolvedUser: props.value?.preferUninvolvedUser === true,
        preferProcessInstanceAssignee: props.value?.preferProcessInstanceAssignee === true,
    };
    const preferredAssigneeOption = resolvePreferredAssigneeOption(currentValue);

    const disableInputs = props.disabled || props.readOnly;
    const headlineText = props.title != null && props.title.trim().length > 0 ? props.title : DEFAULT_HEADLINE;
    const descriptionText = props.description != null && props.description.trim().length > 0 ? props.description : DEFAULT_TEXT;

    const patchValue = (patch: Partial<AssignmentContextValue>) => {
        props.onChange(normalizeValue({
            ...currentValue,
            ...patch,
        }));
    };

    return (
        <Stack spacing={1.5}>
            <Typography variant="h6">
                {headlineText}
            </Typography>

            <Typography
                variant="body1"
                color="text.secondary"
            >
                {descriptionText}
            </Typography>

            <DomainUserSelectFieldComponent
                label={props.domainAndUserSelectionLabel ?? 'Personenkreis'}
                value={currentValue.domainAndUserSelection}
                onChange={(nextDomainAndUserSelection) => {
                    patchValue({
                        domainAndUserSelection: nextDomainAndUserSelection,
                    });
                }}
                placeholder={props.domainAndUserSelectionPlaceholder}
                hint={props.domainAndUserSelectionHint}
                error={props.domainAndUserSelectionError}
                disabled={props.disabled}
                readOnly={props.readOnly}
                required={props.required}
                options={props.options}
                allowedTypes={props.allowedTypes}
                processAccessConstraint={props.processAccessConstraint}
            />

            <RadioFieldComponent
                label="Bevorzugung bei der Zuweisung"
                value={preferredAssigneeOption === 'none' ? undefined : preferredAssigneeOption}
                onChange={(nextValue) => {
                    const selectedOption = (nextValue ?? 'none') as PreferredAssigneeOption;

                    patchValue({
                        preferPreviousTaskAssignee: selectedOption === 'previous',
                        preferUninvolvedUser: selectedOption === 'uninvolved',
                        preferProcessInstanceAssignee: selectedOption === 'processInstance',
                    });
                }}
                options={[
                    {
                        value: 'previous',
                        label: props.preferredPreviousTaskAssigneeLabel ?? 'Bevorzuge Bearbeiter:in vorheriger Aufgabe',
                    },
                    {
                        value: 'uninvolved',
                        label: props.preferredUninvolvedUserLabel ?? 'Bevorzuge eine neue, unbeteiligte Mitarbeiter:in',
                    },
                    {
                        value: 'processInstance',
                        label: props.preferredProcessInstanceAssigneeLabel ?? 'Bevorzuge dem Vorgang zugewiesene Mitarbeiter:in',
                    },
                ]}
                disabled={disableInputs}
            />
        </Stack>
    );
}

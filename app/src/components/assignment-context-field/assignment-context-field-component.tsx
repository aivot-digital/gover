import {Box, Stack, Typography} from '@mui/material';
import {DomainUserSelectFieldComponent} from '../domain-user-select-field/domain-user-select-field-component';
import {
    AssignmentContextValue,
    GeneralAssigneePreference,
    RepeatExecutionAssigneePreference,
} from '../../models/elements/form/input/assignment-context-field-element';
import {DomainAndUserSelectOption} from '../domain-user-select-field/domain-user-select-options';
import {
    DomainAndUserSelectItemType,
    DomainAndUserSelectProcessAccessConstraint,
} from '../../models/elements/form/input/domain-user-select-field-element';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {Hint} from '../hint/hint';
import {
    assignmentContextGeneralAssigneePreferenceOptions,
    assignmentContextRepeatExecutionAssigneePreferenceOptions,
} from '../../utils/assignment-context-preference-options';

export interface AssignmentContextFieldComponentProps {
    value?: AssignmentContextValue | null;
    onChange: (value: AssignmentContextValue | null) => void;

    title?: string;
    description?: string;

    domainAndUserSelectionLabel?: string;
    domainAndUserSelectionHint?: string;
    domainAndUserSelectionPlaceholder?: string;
    domainAndUserSelectionError?: string;

    disabled?: boolean;
    readOnly?: boolean;
    required?: boolean;

    options?: DomainAndUserSelectOption[];
    allowedTypes?: DomainAndUserSelectItemType[] | null;
    processAccessConstraint?: DomainAndUserSelectProcessAccessConstraint | null;
}

const DEFAULT_HEADLINE = 'Verantwortlicher Personenkreis';
const DEFAULT_TEXT = 'Definieren Sie den Personenkreis, der für diese Aufgabe herangezogen werden kann.';
const ASSIGNMENT_LOGIC_SUMMARY = 'Die automatische Zuweisung ermittelt zuerst alle berechtigten Personen aus dem Personenkreis. Danach werden die gewählten Bevorzugungen und abschließend die aktuelle Aufgabenlast berücksichtigt.';

function resolveGeneralAssigneePreference(value: AssignmentContextValue): GeneralAssigneePreference {
    switch (value.generalAssigneePreference) {
        case 'previousProcessStepAssignee':
        case 'uninvolvedUser':
        case 'processInstanceAssignee':
            return value.generalAssigneePreference;
        default:
            return 'none';
    }
}

function resolveRepeatExecutionAssigneePreference(value: AssignmentContextValue): RepeatExecutionAssigneePreference {
    switch (value.repeatExecutionAssigneePreference) {
        case 'previousIterationAssignee':
        case 'differentFromPreviousIterationAssignee':
            return value.repeatExecutionAssigneePreference;
        default:
            return 'none';
    }
}

function normalizeValue(value: AssignmentContextValue): AssignmentContextValue | null {
    const domainAndUserSelection = value.domainAndUserSelection != null && value.domainAndUserSelection.length > 0
        ? value.domainAndUserSelection
        : undefined;

    const generalAssigneePreference = resolveGeneralAssigneePreference(value);
    const repeatExecutionAssigneePreference = resolveRepeatExecutionAssigneePreference(value);
    const hasGeneralAssigneePreference = generalAssigneePreference !== 'none';
    const hasRepeatExecutionAssigneePreference = repeatExecutionAssigneePreference !== 'none';

    if (
        domainAndUserSelection == null &&
        !hasGeneralAssigneePreference &&
        !hasRepeatExecutionAssigneePreference
    ) {
        return null;
    }

    return {
        domainAndUserSelection,
        generalAssigneePreference: hasGeneralAssigneePreference
            ? generalAssigneePreference
            : undefined,
        repeatExecutionAssigneePreference: hasRepeatExecutionAssigneePreference
            ? repeatExecutionAssigneePreference
            : undefined,
    };
}

export function AssignmentContextFieldComponent(props: AssignmentContextFieldComponentProps) {
    const currentValue: AssignmentContextValue = {
        domainAndUserSelection: props.value?.domainAndUserSelection,
        generalAssigneePreference: props.value == null
            ? 'none'
            : resolveGeneralAssigneePreference(props.value),
        repeatExecutionAssigneePreference: props.value == null
            ? 'none'
            : resolveRepeatExecutionAssigneePreference(props.value),
    };
    const generalAssigneePreference = resolveGeneralAssigneePreference(currentValue);
    const repeatExecutionAssigneePreference = resolveRepeatExecutionAssigneePreference(currentValue);

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
                {headlineText}{props.required ? ' *' : ''}
            </Typography>

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 0.5,
                }}
            >
                <Typography
                    variant="body1"
                    color="text.secondary"
                    sx={{
                        flex: 1,
                    }}
                >
                    {descriptionText}
                </Typography>

                <Hint
                    summary={ASSIGNMENT_LOGIC_SUMMARY}
                    detailsTitle="Zuweisungslogik"
                    details={
                        <Box>
                            <Typography variant="body2" sx={{mb: 2}}>
                                {ASSIGNMENT_LOGIC_SUMMARY}
                            </Typography>

                            <Typography variant="body2" sx={{mb: 2}}>
                                Die Zuweisung erfolgt in dieser Reihenfolge:
                            </Typography>

                            <Box
                                component="ol"
                                sx={{
                                    m: 0,
                                    pl: 3,
                                }}
                            >
                                <Typography component="li" variant="body2" sx={{mb: 1}}>
                                    Aus dem ausgewählten Personenkreis werden alle Mitarbeitenden ermittelt, die für diese Aufgabe berechtigt sind.
                                </Typography>
                                <Typography component="li" variant="body2" sx={{mb: 1}}>
                                    Wenn dieselbe Aufgabe im Rahmen einer Schleife erneut ausgeführt wird, wird die Bevorzugung bei erneuter Ausführung berücksichtigt.
                                </Typography>
                                <Typography component="li" variant="body2" sx={{mb: 1}}>
                                    Danach wird die allgemeine Bevorzugung bei der Zuweisung angewendet.
                                </Typography>
                                <Typography component="li" variant="body2" sx={{mb: 1}}>
                                    Wenn eine Bevorzugung nicht angewendet werden kann, bleibt die bisher ermittelte Personenauswahl unverändert.
                                </Typography>
                                <Typography component="li" variant="body2">
                                    Wenn mehrere Personen infrage kommen, wird die Person mit der geringsten aktiven Aufgabenlast ausgewählt.
                                </Typography>
                            </Box>
                        </Box>
                    }
                    sx={{
                        mt: -0.5,
                    }}
                />
            </Box>

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

            <Stack spacing={2}>
                <SelectFieldComponent
                    label="Bevorzugung bei der Zuweisung"
                    value={generalAssigneePreference}
                    onChange={(nextValue) => {
                        patchValue({
                            generalAssigneePreference: (nextValue ?? 'none') as GeneralAssigneePreference,
                        });
                    }}
                    options={assignmentContextGeneralAssigneePreferenceOptions}
                    includeEmptyOption={false}
                    disabled={props.disabled}
                    readOnly={props.readOnly}
                    size="small"
                />

                <SelectFieldComponent
                    label="Bevorzugung bei erneuter Ausführung (Schleife)"
                    value={repeatExecutionAssigneePreference}
                    onChange={(nextValue) => {
                        patchValue({
                            repeatExecutionAssigneePreference: (nextValue ?? 'none') as RepeatExecutionAssigneePreference,
                        });
                    }}
                    options={assignmentContextRepeatExecutionAssigneePreferenceOptions}
                    includeEmptyOption={false}
                    disabled={props.disabled}
                    readOnly={props.readOnly}
                    size="small"
                />
            </Stack>
        </Stack>
    );
}

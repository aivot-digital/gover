import {Button, Stack} from '@mui/material';
import SwapHorizOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SwapHoriz';
import {useState} from 'react';
import {FormFieldGroup} from '../form-field';
import {TableFieldComponent2} from '../table-field/table-field-component-2';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type OptionListInputProps, type OptionListInputValue} from './option-list-input-props';

export function OptionListInput(props: OptionListInputProps) {
    const [textInputMode, setTextInputMode] = useState(false);
    const [textInputBuffer, setTextInputBuffer] = useState<string>();
    const options = props.value ?? [];
    const groupFieldEnabled = props.showGroupField === true;
    const isKeyFieldDisabled = props.disableKeyField === true;
    const isInteractionDisabled = Boolean(props.disabled || props.busy || props.readOnly);
    const hasNotEnoughItems = !props.allowEmpty && options.length === 0;
    const hasEmptyField = options.some((option) => (
        option.label.trim().length === 0 ||
        (!isKeyFieldDisabled && option.value.trim().length === 0)
    ));
    const hasDuplicateLabels = new Set(options.map((option) => option.label)).size !== options.length;
    const hasDuplicateValues = new Set(options.map((option) => option.value)).size !== options.length;
    const validationError = getValidationError({
        hasNotEnoughItems,
        hasEmptyField,
        hasDuplicateLabels,
        hasDuplicateValues,
    });
    const resolvedError = props.error ?? validationError;

    if (textInputMode) {
        return (
            <FormFieldGroup
                id={props.id}
                label={props.label}
                hint={props.hint}
                error={resolvedError}
                required={!props.allowEmpty}
                disabled={props.disabled}
                busy={props.busy}
                readOnly={props.readOnly}
                ariaDescribedBy={props.ariaDescribedBy}
                margin={props.margin}
                showOptionalIndicator={props.showOptionalIndicator}
                sx={props.sx}
                labelAction={(group) => {
                    const suppliedLabelAction = typeof props.labelAction === 'function'
                        ? props.labelAction(group)
                        : props.labelAction;

                    if (isInteractionDisabled && suppliedLabelAction == null) {
                        return null;
                    }

                    return (
                        <Stack direction="row" spacing={0.5} sx={{alignItems: 'center'}}>
                            {suppliedLabelAction}
                            {!isInteractionDisabled && (
                                <Button
                                    size="small"
                                    startIcon={<SwapHorizOutlinedIcon/>}
                                    onClick={() => setTextInputMode(false)}
                                >
                                    Tabellenansicht
                                </Button>
                            )}
                        </Stack>
                    );
                }}
            >
                {(group) => (
                    <TextFieldComponent
                        label="Einträge"
                        placeholder={getTextInputPlaceholder(isKeyFieldDisabled, groupFieldEnabled)}
                        value={textInputBuffer ?? serializeOptions(options, isKeyFieldDisabled, groupFieldEnabled)}
                        onChange={(value) => setTextInputBuffer(value ?? '')}
                        onBlur={(value) => {
                            setTextInputBuffer(undefined);
                            props.onChange(value == null
                                ? undefined
                                : parseOptions(value, isKeyFieldDisabled, groupFieldEnabled));
                        }}
                        multiline
                        required={!props.allowEmpty}
                        disabled={props.disabled}
                        busy={props.busy}
                        readonly={props.readOnly}
                        ariaDescribedBy={group.describedBy}
                        showOptionalIndicator={false}
                        margin="none"
                        controlSx={props.controlSx}
                    />
                )}
            </FormFieldGroup>
        );
    }

    const fields = [
        {
            key: 'label' as const,
            label: props.labelLabel ?? 'Beschriftung',
            type: 'string' as const,
            required: true,
            disabled: props.disabled,
        },
        ...(!isKeyFieldDisabled ? [{
            key: 'value' as const,
            label: props.keyLabel ?? 'Wert',
            type: 'string' as const,
            required: true,
            disabled: props.disabled,
        }] : []),
        ...(groupFieldEnabled ? [{
            key: 'group' as const,
            label: props.groupLabel ?? 'Gruppe',
            type: 'string' as const,
            disabled: props.disabled,
        }] : []),
    ];

    return (
        <TableFieldComponent2<OptionListInputValue>
            id={props.id}
            label={props.label}
            hint={props.hint}
            error={resolvedError}
            noRowsPlaceholder={props.noItemsHint}
            fields={fields}
            createDefaultRow={() => ({
                label: '',
                value: '',
                group: groupFieldEnabled ? '' : undefined,
            })}
            value={options}
            onChange={(value) => {
                props.onChange(value == null ? undefined : value.map((option) => ({
                    ...option,
                    value: isKeyFieldDisabled ? option.label : option.value,
                    group: groupFieldEnabled ? option.group : undefined,
                })));
            }}
            disabled={props.disabled}
            busy={props.busy}
            readOnly={props.readOnly}
            required={!props.allowEmpty}
            addLabel={props.addLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            margin={props.margin}
            sx={props.sx}
            showOptionalIndicator={props.showOptionalIndicator}
            actions={[{
                icon: <SwapHorizOutlinedIcon/>,
                iconPosition: 'start',
                label: 'Textansicht',
                tooltip: 'Einträge als Text bearbeiten',
                ariaLabel: `${props.label}: Einträge als Text bearbeiten`,
                onClick: () => setTextInputMode(true),
                visible: !isInteractionDisabled,
            }]}
            controlSx={props.controlSx}
        />
    );
}

function getValidationError(state: {
    hasNotEnoughItems: boolean;
    hasEmptyField: boolean;
    hasDuplicateLabels: boolean;
    hasDuplicateValues: boolean;
}): string | undefined {
    if (state.hasEmptyField) {
        return 'Jede Zeile muss alle erforderlichen Werte enthalten. Befüllen Sie fehlende Werte oder entfernen Sie die entsprechenden Zeilen.';
    }
    if (state.hasNotEnoughItems) {
        return 'Bitte fügen Sie mindestens einen Wert hinzu.';
    }
    if (state.hasDuplicateLabels && state.hasDuplicateValues) {
        return 'Beschriftungen und Werte müssen jeweils eindeutig sein.';
    }
    if (state.hasDuplicateLabels) {
        return 'Die Beschriftungen müssen eindeutig sein.';
    }
    if (state.hasDuplicateValues) {
        return 'Die Werte müssen eindeutig sein.';
    }
    return undefined;
}

function getTextInputPlaceholder(disableKeyField: boolean, showGroupField: boolean): string {
    if (disableKeyField) {
        return showGroupField
            ? 'Beschriftung 1|Gruppe 1\nBeschriftung 2|Gruppe 2'
            : 'Beschriftung 1\nBeschriftung 2';
    }
    return showGroupField
        ? 'Beschriftung 1|Wert 1|Gruppe 1\nBeschriftung 2|Wert 2|Gruppe 2'
        : 'Beschriftung 1|Wert 1\nBeschriftung 2|Wert 2';
}

function serializeOptions(
    options: OptionListInputValue[],
    disableKeyField: boolean,
    showGroupField: boolean,
): string {
    return options.map((option) => [
        option.label,
        ...(!disableKeyField ? [option.value] : []),
        ...(showGroupField ? [option.group ?? ''] : []),
    ].join('|')).join('\n');
}

function parseOptions(
    value: string,
    disableKeyField: boolean,
    showGroupField: boolean,
): OptionListInputValue[] {
    return value.split('\n').map((line) => {
        const parts = line.split('|');
        const label = (parts[0] ?? '').trim();
        const valueIndex = disableKeyField ? 0 : 1;
        const groupIndex = disableKeyField ? 1 : 2;

        return {
            label,
            value: disableKeyField ? label : (parts[valueIndex] ?? '').trim(),
            group: showGroupField ? (parts[groupIndex] ?? '').trim() : undefined,
        };
    });
}

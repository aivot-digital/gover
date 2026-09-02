import {Button, Stack} from '@mui/material';
import React, {useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import SwapHorizOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SwapHoriz';
import {TableFieldComponent2} from '../table-field/table-field-component-2';
import {type StringListInputProps} from './string-list-input-props';
import {FormFieldGroup} from '../form-field';

export function StringListInput(props: StringListInputProps) {
    const {
        controlSx,
        label,
        hint,
        addLabel,
        noItemsHint,
        value,
        onChange,
        allowEmpty,
        disabled,
        busy,
        readOnly,
        error,
    } = props;

    const [rawMode, setRawMode] = useState(false);
    const [rawBuffer, setRawBuffer] = useState<string>();
    const isValueEmpty = !allowEmpty && (value == null || value.length === 0);
    const hasEmptyItem = (value ?? []).some((val) => val.trim().length === 0);
    const validationError = hasEmptyItem
        ? 'Jede Zeile muss einen Wert enthalten. Befüllen Sie fehlende Werte oder entfernen Sie die entsprechenden Zeilen.'
        : (isValueEmpty ? 'Bitte fügen Sie mindestens einen Wert hinzu.' : undefined);
    const resolvedError = error ?? validationError;
    const isInteractionDisabled = Boolean(disabled || busy || readOnly);

    if (rawMode) {
        return (
            <FormFieldGroup
                id={props.id}
                label={label}
                hint={hint}
                error={resolvedError}
                required={!allowEmpty}
                disabled={disabled}
                busy={busy}
                readOnly={readOnly}
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
                                    onClick={() => setRawMode(false)}
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
                        placeholder={'Option 1\nOption 2\nOption 3'}
                        value={rawBuffer ?? (value ?? []).join('\n')}
                        onChange={(val) => setRawBuffer(val ?? '')}
                        onBlur={(val) => {
                            setRawBuffer(undefined);
                            onChange(val != null ? val.split('\n').map((line) => line.trim()) : undefined);
                        }}
                        multiline
                        required={!allowEmpty}
                        disabled={disabled}
                        busy={busy}
                        readonly={readOnly}
                        ariaDescribedBy={group.describedBy}
                        showOptionalIndicator={false}
                        margin="none"
                        controlSx={controlSx}
                    />
                )}
            </FormFieldGroup>
        );
    } else {
        return (
            <TableFieldComponent2
                id={props.id}
                ariaDescribedBy={props.ariaDescribedBy}
                labelAction={props.labelAction}
                margin={props.margin}
                sx={props.sx}
                showOptionalIndicator={props.showOptionalIndicator}
                label={label}
                hint={hint}
                error={resolvedError}
                noRowsPlaceholder={noItemsHint}
                addLabel={addLabel}
                fields={[{
                    label: 'Eintrag',
                    type: 'string',
                    key: 'value',
                    disabled: disabled,
                    required: true,
                }]}
                disabled={disabled}
                busy={busy}
                readOnly={readOnly}
                required={!allowEmpty}
                createDefaultRow={() => ({
                    value: '',
                })}
                value={(value ?? []).map(val => ({
                    value: val,
                }))}
                onChange={(val) => {
                    onChange(val == null ? undefined : val.map((v) => v.value));
                }}
                actions={[{
                    icon: <SwapHorizOutlinedIcon />,
                    iconPosition: 'start',
                    label: 'Textansicht',
                    tooltip: 'Einträge als Text bearbeiten',
                    ariaLabel: `${label}: Einträge als Text bearbeiten`,
                    onClick: () => {
                        setRawMode(true);
                    },
                    visible: !isInteractionDisabled,
                }]}
                controlSx={controlSx}
            />
        );
    }
}

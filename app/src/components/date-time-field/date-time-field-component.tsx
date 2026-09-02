import {InputAdornment, type SxProps, type Theme} from '@mui/material';
import {LocalizationProvider, DateTimePicker} from '@mui/x-date-pickers';
import {DateTime} from 'luxon';
import React, {type ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import {ProsunaAdapterLuxon, NONEXISTENT_LOCAL_DATETIME_REASON} from '../../utils/prosuna-adapter-luxon';
import {
    canonicalizeInstant,
    getApplicationTimeZone,
    instantToDateTime,
    resolvePickerDateTime,
    TemporalPrecision,
} from '../../utils/temporal-utils';
import {InstantIso} from '../../utils/temporal-types';
import {type EndAction} from '../text-field/text-field-component-props';
import {renderIconButton} from '../text-field/text-field-component';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {FormField, type FormFieldControlContext, type FormFieldLayoutProps} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export interface DateTimeFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    value?: string | null;
    onChange: (value: InstantIso | null) => void;
    onBlur?: (value: InstantIso | null) => void;
    hint?: string;
    hideHelperText?: boolean;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    placeholder?: string;
    controlSx?: SxProps<Theme>;
    size?: 'small' | 'medium';
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    mode?: TimeFieldComponentModelMode;
    endAction?: EndAction | EndAction[];
    startIcon?: ReactNode;
}

function getPrecision(mode: TimeFieldComponentModelMode): TemporalPrecision {
    return mode === TimeFieldComponentModelMode.Second ? 'second' : 'minute';
}

export function DateTimeFieldComponent(props: DateTimeFieldComponentProps) {
    const mode = props.mode ?? TimeFieldComponentModelMode.Minute;
    const precision = getPrecision(mode);
    const applicationTimeZone = useMemo(() => getApplicationTimeZone(), []);
    const dateValue = props.value ? instantToDateTime(props.value, applicationTimeZone) : null;
    const [localValue, setLocalValue] = useState<DateTime | null>(dateValue);
    const [temporalError, setTemporalError] = useState<string | null>(null);
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // MUI fires picker changes for both popover selection and direct field edits.
    // We track keyboard-driven edits separately so clearing the text field also
    // propagates an explicit clear to the parent instead of only mutating local picker state.
    const lastInputWasTypingRef = useRef(false);

    useEffect(() => {
        const parsed = props.value ? instantToDateTime(props.value, applicationTimeZone) : null;
        setLocalValue(parsed);
        setTemporalError(null);
    }, [applicationTimeZone, props.value]);

    useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const effectiveError = temporalError ?? props.error;

    const triggerChange = (date: DateTime | null) => {
        if (date === null) {
            setTemporalError(null);
            props.onChange(null);
            props.onBlur?.(null);
            return;
        }

        if (!date.isValid) {
            setTemporalError(
                date.invalidReason === NONEXISTENT_LOCAL_DATETIME_REASON
                    ? 'Diese lokale Uhrzeit existiert wegen der Zeitumstellung nicht.'
                    : 'Das eingegebene Datum oder die eingegebene Uhrzeit ist ungültig.',
            );
            return;
        }

        const resolution = resolvePickerDateTime(date, applicationTimeZone, precision);

        if (!resolution.resolved) {
            setTemporalError(
                resolution.reason === 'nonexistent'
                    ? 'Diese lokale Uhrzeit existiert wegen der Zeitumstellung nicht.'
                    : 'Das eingegebene Datum oder die eingegebene Uhrzeit ist ungültig.',
            );
            return;
        }

        setTemporalError(null);
        props.onChange(resolution.value);
        props.onBlur?.(resolution.value);
    };

    const handleChange = (newDate: DateTime | null) => {
        setLocalValue(newDate);

        // Popover interactions are committed on accept. Text input changes must be
        // forwarded immediately (or via blur/debounce) so manual clearing is persisted.
        if (!lastInputWasTypingRef.current) {
            return;
        }

        if (props.bufferInputUntilBlur) {
            return;
        }

        if (props.debounce) {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
            debounceTimeoutRef.current = setTimeout(() => {
                triggerChange(newDate);
            }, props.debounce);
        } else {
            triggerChange(newDate);
        }
    };

    const handlePickerChange = (newDate: unknown) => {
        if (newDate === null || DateTime.isDateTime(newDate)) {
            handleChange(newDate);
        }
    };

    const handleAccept = (acceptedDate: unknown) => {
        if (lastInputWasTypingRef.current) {
            return;
        }

        const pickedDate = acceptedDate === null || DateTime.isDateTime(acceptedDate)
            ? acceptedDate
            : null;
        const currentIso = props.value
            ? canonicalizeInstant(props.value, applicationTimeZone, precision)
            : null;
        const pickedResolution = pickedDate
            ? resolvePickerDateTime(pickedDate, applicationTimeZone, precision)
            : null;
        const pickedIso = pickedResolution?.resolved ? pickedResolution.value : null;

        if (currentIso !== pickedIso) {
            triggerChange(pickedDate);
        }
    };

    const handleOpen = () => {
        lastInputWasTypingRef.current = false;
    };

    const handleBlur = () => {
        if (lastInputWasTypingRef.current && props.bufferInputUntilBlur) {
            triggerChange(localValue);
        }
    };

    const handleInputChange = () => {
        lastInputWasTypingRef.current = true;
    };

    const handleKeyDown = (event: React.KeyboardEvent) => {
        if (event.key.length === 1 || event.key === 'Backspace' || event.key === 'Delete') {
            lastInputWasTypingRef.current = true;
        }
    };

    return (
        <FormField
            id={props.id}
            label={props.label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={props.hint}
            error={effectiveError}
            hideHelperText={props.hideHelperText && temporalError == null}
            required={props.required}
            disabled={props.disabled}
            readOnly={props.busy}
            busy={props.busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(fieldContext: FormFieldControlContext) => (
                <LocalizationProvider
                    dateAdapter={ProsunaAdapterLuxon}
                    adapterLocale="de"
                >
                    <DateTimePicker
                        ampm={false}
                        timezone={applicationTimeZone}
                        format={mode === TimeFieldComponentModelMode.Second ? "dd.MM.yyyy HH:mm:ss 'Uhr'" : "dd.MM.yyyy HH:mm 'Uhr'"}
                        views={mode === TimeFieldComponentModelMode.Second ? ['year', 'month', 'day', 'hours', 'minutes', 'seconds'] : ['year', 'month', 'day', 'hours', 'minutes']}
                        label={undefined}
                        value={localValue}
                        onChange={handlePickerChange}
                        onAccept={handleAccept}
                        onOpen={handleOpen}
                        disabled={props.disabled}
                        readOnly={props.busy}
                        slotProps={{
                            textField: {
                                id: fieldContext.controlId,
                                variant: 'outlined',
                                fullWidth: true,
                                margin: 'none',
                                size: props.size ?? 'small',
                                required: props.required,
                                error: fieldContext.invalid,
                                helperText: undefined,
                                onInput: handleInputChange,
                                onKeyDown: handleKeyDown,
                                onPaste: handleInputChange,
                                onBlur: handleBlur,
                                slotProps: {
                                    input: {
                                        ...fieldContext.ariaProps,
                                        startAdornment: props.startIcon && (
                                            <InputAdornment position="start">{props.startIcon}</InputAdornment>
                                        ),
                                        endAdornment: props.endAction && (
                                            <InputAdornment position="end">
                                                {Array.isArray(props.endAction)
                                                    ? props.endAction.map(renderIconButton)
                                                    : renderIconButton(props.endAction)}
                                            </InputAdornment>
                                        ),
                                    },
                                    htmlInput: {
                                        placeholder: props.placeholder,
                                    },
                                },
                            },
                            actionBar: {
                                actions: ['accept', 'cancel', 'clear'],
                            },
                        }}
                        sx={[
                            {
                                width: '100%',
                                '& .MuiPickersInputBase-root': {
                                    minHeight: FormFieldTokens.controlMinHeight,
                                    backgroundColor: (props.busy || props.disabled) ? getDisabledFieldBackground : undefined,
                                    cursor: (props.busy || props.disabled) ? 'not-allowed' : undefined,
                                },
                            },
                            ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                        ]}
                    />
                </LocalizationProvider>
            )}
        </FormField>
    );
}

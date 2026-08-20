import {InputAdornment, SxProps, Theme} from '@mui/material';
import {LocalizationProvider, TimePicker} from '@mui/x-date-pickers';
import {DateTime} from 'luxon';
import React, {ReactNode, useEffect, useRef, useState} from 'react';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import {ProsunaAdapterLuxon} from '../../utils/prosuna-adapter-luxon';
import {
    dateTimeToLocalTimeIso,
    localTimeIsoToDateTime,
    TemporalPrecision,
} from '../../utils/temporal-utils';
import {LocalTimeIso} from '../../utils/temporal-types';
import {EndAction} from '../text-field/text-field-component-props';
import {renderIconButton} from '../text-field/text-field-component';

interface TimeFieldComponentProps {
    label: string;
    value?: string | null;
    onChange: (value: LocalTimeIso | null) => void;
    onBlur?: (val: LocalTimeIso | null) => void;
    autocomplete?: string;
    hint?: string;
    hideHelperText?: boolean;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    mode?: TimeFieldComponentModelMode;
    endAction?: EndAction | EndAction[];
    startIcon?: ReactNode;
}

export function TimeFieldComponent(props: TimeFieldComponentProps) {
    const mode = props.mode ?? TimeFieldComponentModelMode.Minute;
    const precision = mode as TemporalPrecision;
    const dateValue = props.value != null ? localTimeIsoToDateTime(props.value) : null;
    const [localValue, setLocalValue] = useState<DateTime | null>(dateValue);
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // MUI uses the same change callback for direct text edits and popover selection.
    // Text edits follow debounce/blur settings; popover selections are committed on accept.
    const lastInputWasTypingRef = useRef(false);

    useEffect(() => {
        const parsed = props.value != null ? localTimeIsoToDateTime(props.value) : null;
        setLocalValue(parsed);
    }, [props.value]);

    useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const triggerChange = (date: DateTime | null) => {
        if (date === null) {
            props.onChange(null);
            props.onBlur?.(null);
            return;
        }

        const timeIso = dateTimeToLocalTimeIso(date, precision);
        if (timeIso !== null) {
            props.onChange(timeIso);
            props.onBlur?.(timeIso);
        }
    };

    const handleChange = (newDate: DateTime | null) => {
        setLocalValue(newDate);

        if (!lastInputWasTypingRef.current || props.bufferInputUntilBlur) {
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
        const currentIso = props.value != null
            ? dateTimeToLocalTimeIso(localTimeIsoToDateTime(props.value) ?? DateTime.invalid('invalid'), precision)
            : null;
        const pickedIso = pickedDate != null
            ? dateTimeToLocalTimeIso(pickedDate, precision)
            : null;

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
        <LocalizationProvider
            dateAdapter={ProsunaAdapterLuxon}
            adapterLocale="de"
        >
            <TimePicker
                ampm={false}
                // Floating times are zone-free. UTC is only a stable MUI carrier that
                // prevents DST or the browser timezone from changing their clock fields.
                timezone="UTC"
                format={mode === TimeFieldComponentModelMode.Second ? "HH:mm:ss 'Uhr'" : "HH:mm 'Uhr'"}
                views={mode === TimeFieldComponentModelMode.Second ? ['hours', 'minutes', 'seconds'] : ['hours', 'minutes']}
                label={`${props.label}${props.required ? ' *' : ''}`}
                value={localValue}
                onChange={handlePickerChange}
                onAccept={handleAccept}
                onOpen={handleOpen}
                disabled={props.disabled}
                readOnly={props.busy}
                slotProps={{
                    textField: {
                        variant: 'outlined',
                        error: props.error != null,
                        helperText: props.hideHelperText ? undefined : props.error ?? props.hint,
                        InputLabelProps: {
                            title: props.label,
                        },
                        autoComplete: props.autocomplete,
                        onInput: handleInputChange,
                        onKeyDown: handleKeyDown,
                        onPaste: handleInputChange,
                        onBlur: handleBlur,
                        InputProps: {
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
                    },
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}
                sx={{
                    ...props.sx,
                    '& .MuiPickersInputBase-root': {
                        backgroundColor: (props.busy || props.disabled) ? '#F8F8F8' : undefined,
                        cursor: (props.busy || props.disabled) ? 'not-allowed' : undefined,
                    },
                }}
            />
        </LocalizationProvider>
    );
}

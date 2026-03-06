import {LocalizationProvider, TimePicker} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {de} from 'date-fns/locale/de';
import {useEffect, useRef, useState} from 'react';
import {SxProps, Theme} from '@mui/material';
import type {Locale} from 'date-fns';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';

const deLocale = de as unknown as Locale;

interface TimeFieldComponentProps {
    label: string;
    value?: string;
    onChange: (value: string | undefined) => void;
    onBlur?: (val: string | undefined) => void;
    autocomplete?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    mode?: TimeFieldComponentModelMode;
}

export function TimeFieldComponent(props: TimeFieldComponentProps) {
    const mode = props.mode ?? TimeFieldComponentModelMode.Minute;
    const dateValue = props.value ? new Date(props.value) : null;
    const [localValue, setLocalValue] = useState<Date | null>(dateValue);
    const [lastInputWasTyping, setLastInputWasTyping] = useState(false);
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const lastPickerValueRef = useRef<Date | null>(dateValue);

    useEffect(() => {
        const parsed = props.value ? new Date(props.value) : null;
        setLocalValue(parsed);
        lastPickerValueRef.current = parsed;
    }, [props.value]);

    const triggerChange = (date: Date | null) => {
        if (date === null) {
            props.onChange(undefined);
            props.onBlur?.(undefined);
            return;
        }

        if (date instanceof Date && !isNaN(date.getTime())) {
            const iso = date.toISOString();
            props.onChange(iso);
            props.onBlur?.(iso);
        }
    };

    const handleChange = (newDate: Date | null) => {
        setLocalValue(newDate);
        lastPickerValueRef.current = newDate;

        if (!lastInputWasTyping) return;

        if (props.bufferInputUntilBlur) return;

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

    const handleAccept = () => {
        //triggerChange(lastPickerValueRef.current);
    };

    const handleClose = () => {
        if (lastInputWasTyping) return;

        const currentIso = props.value ?? null;
        const pickedIso = lastPickerValueRef.current?.toISOString() ?? null;

        if (currentIso !== pickedIso) {
            triggerChange(lastPickerValueRef.current);
        }
    };

    const handleOpen = () => {
        setLastInputWasTyping(false);
    };

    const handleBlur = () => {
        if (lastInputWasTyping && props.bufferInputUntilBlur) {
            triggerChange(localValue);
        }
    };

    return (
        <LocalizationProvider
            dateAdapter={AdapterDateFns}
            adapterLocale={deLocale}
        >
            <TimePicker
                format={mode === TimeFieldComponentModelMode.Second ? "HH:mm:ss 'Uhr'" : "HH:mm 'Uhr'"}
                views={mode === TimeFieldComponentModelMode.Second ? ['hours', 'minutes', 'seconds'] : ['hours', 'minutes']}
                label={`${props.label}${props.required ? ' *' : ''}`}
                value={localValue}
                onChange={handleChange}
                onAccept={handleAccept}
                onClose={handleClose}
                onOpen={handleOpen}
                disabled={props.disabled}
                readOnly={props.busy}
                slotProps={{
                    textField: {
                        variant: 'outlined',
                        error: props.error != null,
                        helperText: props.error != null ? props.error : props.hint,
                        InputLabelProps: {
                            title: props.label,
                        },
                        autoComplete: props.autocomplete,
                        onInput: () => setLastInputWasTyping(true),
                        onBlur: handleBlur,
                    },
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}
                sx={{
                    ...props.sx,
                    "& .MuiPickersInputBase-root": {
                        backgroundColor: (props.busy || props.disabled) ? "#F8F8F8" : undefined,
                        cursor: (props.busy || props.disabled) ? "not-allowed" : undefined,
                    },
                }}
            />
        </LocalizationProvider>
    );
}

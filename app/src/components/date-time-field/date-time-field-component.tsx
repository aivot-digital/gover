import {LocalizationProvider, DateTimePicker} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {de} from 'date-fns/locale/de';
import type {Locale} from 'date-fns';
import {useEffect, useMemo, useRef, useState} from 'react';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';

const deLocale = de as unknown as Locale;

interface DateTimeFieldComponentProps {
    label: string;
    value?: string;
    onChange: (value: string | undefined) => void;
    onBlur?: (value: string | undefined) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    placeholder?: string;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    mode?: TimeFieldComponentModelMode;
}

export function DateTimeFieldComponent(props: DateTimeFieldComponentProps) {
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

    const helperText = useMemo(() => {
        return props.error != null ? props.error : props.hint;
    }, [props.error, props.hint]);

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

        if (!lastInputWasTyping) {
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

    const handleClose = () => {
        if (lastInputWasTyping) {
            return;
        }

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
            <DateTimePicker
                ampm={false}
                format={mode === TimeFieldComponentModelMode.Second ? "dd.MM.yyyy HH:mm:ss 'Uhr'" : "dd.MM.yyyy HH:mm 'Uhr'"}
                views={mode === TimeFieldComponentModelMode.Second ? ['year', 'month', 'day', 'hours', 'minutes', 'seconds'] : ['year', 'month', 'day', 'hours', 'minutes']}
                label={`${props.label}${props.required ? ' *' : ''}`}
                value={localValue}
                onChange={handleChange}
                onClose={handleClose}
                onOpen={handleOpen}
                disabled={props.disabled}
                readOnly={props.busy}
                slotProps={{
                    textField: {
                        variant: 'outlined',
                        error: props.error != null,
                        helperText: helperText,
                        InputLabelProps: {
                            title: props.label,
                        },
                        placeholder: props.placeholder,
                        onInput: () => setLastInputWasTyping(true),
                        onBlur: handleBlur,
                    },
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}
                sx={{
                    '& .MuiPickersInputBase-root': {
                        backgroundColor: (props.busy || props.disabled) ? '#F8F8F8' : undefined,
                        cursor: (props.busy || props.disabled) ? 'not-allowed' : undefined,
                    },
                }}
            />
        </LocalizationProvider>
    );
}

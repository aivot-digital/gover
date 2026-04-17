import {LocalizationProvider, DateTimePicker} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {de} from 'date-fns/locale/de';
import type {Locale} from 'date-fns';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';

const deLocale = de as unknown as Locale;

interface DateTimeFieldComponentProps {
    label: string;
    value?: string;
    onChange: (value: string | undefined) => void;
    onBlur?: (value: string | undefined) => void;
    hint?: string;
    hideHelperText?: boolean;
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
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // MUI fires picker changes for both popover selection and direct field edits.
    // We track keyboard-driven edits separately so clearing the text field also
    // propagates `undefined` to the parent instead of only mutating local picker state.
    const lastInputWasTypingRef = useRef(false);
    const lastPickerValueRef = useRef<Date | null>(dateValue);

    useEffect(() => {
        const parsed = props.value ? new Date(props.value) : null;
        setLocalValue(parsed);
        lastPickerValueRef.current = parsed;
    }, [props.value]);

    useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const helperText = useMemo(() => {
        return props.hideHelperText ? undefined : props.error != null ? props.error : props.hint;
    }, [props.error, props.hideHelperText, props.hint]);

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

        // Popover interactions are committed on close. Text input changes must be
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

    const handleClose = () => {
        if (lastInputWasTypingRef.current) {
            return;
        }

        const currentIso = props.value ?? null;
        const pickedIso = lastPickerValueRef.current?.toISOString() ?? null;

        if (currentIso !== pickedIso) {
            triggerChange(lastPickerValueRef.current);
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
                        onInput: handleInputChange,
                        onKeyDown: handleKeyDown,
                        onPaste: handleInputChange,
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

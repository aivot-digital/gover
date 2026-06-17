import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DatePicker, LocalizationProvider} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {de} from 'date-fns/locale/de';
import {DateFieldComponentProps} from './date-field-component-props';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import type {Locale} from 'date-fns';
import {InputAdornment} from '@mui/material';
import {renderIconButton} from '../text-field/text-field-component';

const deLocale = de as unknown as Locale;

const formatMap = {
    [DateFieldComponentModelMode.Day]: 'dd.MM.yyyy',
    [DateFieldComponentModelMode.Month]: 'MM.yyyy',
    [DateFieldComponentModelMode.Year]: 'yyyy',
};

const viewsMap: {
    [k in DateFieldComponentModelMode]: ('day' | 'month' | 'year')[];
} = {
    [DateFieldComponentModelMode.Day]: ['day', 'month', 'year'],
    [DateFieldComponentModelMode.Month]: ['month', 'year'],
    [DateFieldComponentModelMode.Year]: ['year'],
};

function normalizeDateForMode(date: Date, mode: DateFieldComponentModelMode): Date {
    const normalized = new Date(date);

    if (mode === DateFieldComponentModelMode.Year) {
        normalized.setMonth(0, 1);
    } else if (mode === DateFieldComponentModelMode.Month) {
        normalized.setDate(1);
    }

    normalized.setHours(0, 0, 0, 0);
    return normalized;
}

export function DateFieldComponent({
                                       label,
                                       error,
                                       hint,
                                       hideHelperText,
                                       required,
                                       disabled,
                                       busy,
                                       value,
                                       minDate,
                                       maxDate,
                                       mode,
                                       onChange,
                                       onBlur,
                                       autocomplete,
                                       sx,
                                       bufferInputUntilBlur,
                                       debounce,
                                       muiPassTroughProps,
                                       startIcon,
                                       endAction,
                                   }: DateFieldComponentProps) {
    const dateValue = value != null ? new Date(value) : null;
    const [localValue, setLocalValue] = useState<Date | null>(dateValue);
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // MUI fires picker changes for both popover selection and direct field edits.
    // We track keyboard-driven edits separately so clearing the text field also
    // propagates an explicit clear to the parent instead of only mutating local picker state.
    const lastInputWasTypingRef = useRef(false);
    const lastPickerValueRef = useRef<Date | null>(dateValue);

    useEffect(() => {
        const parsed = value ? new Date(value) : null;
        setLocalValue(parsed);
        lastPickerValueRef.current = parsed;
    }, [value]);

    useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const computedLabel = useMemo(() => {
        let computedLabel = label;
        if (required) {
            if (computedLabel) {
                computedLabel += ' *';
            } else {
                computedLabel = '*';
            }
        }
        return computedLabel;
    }, [label, mode, required]);

    const format = useMemo(() => formatMap[mode ?? DateFieldComponentModelMode.Day], [mode]);
    const views = useMemo(() => viewsMap[mode ?? DateFieldComponentModelMode.Day], [mode]);
    const opensTo = useMemo(() => mode ?? 'day', [mode]);
    const helper = useMemo(() => hideHelperText ? undefined : error != null ? error : hint, [error, hideHelperText, hint]);

    const triggerChange = (date: Date | null) => {
        if (date === null) {
            onChange(null);
            onBlur?.(null);
            return;
        }

        if (date instanceof Date && !isNaN(date.getTime())) {
            const iso = normalizeDateForMode(date, mode ?? DateFieldComponentModelMode.Day).toISOString();
            onChange(iso);
            onBlur?.(iso);
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

        if (bufferInputUntilBlur) {
            return;
        }

        if (debounce) {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
            debounceTimeoutRef.current = setTimeout(() => {
                triggerChange(newDate);
            }, debounce);
        } else {
            triggerChange(newDate);
        }
    };

    const handleAccept = (acceptedDate: Date | null) => {
        //triggerChange(acceptedDate);
    };

    const handleBlur = () => {
        if (lastInputWasTypingRef.current && bufferInputUntilBlur) {
            triggerChange(localValue);
        }
    };

    const handleClose = () => {
        if (lastInputWasTypingRef.current) return;

        const currentIso = value ?? null;
        const pickedIso = lastPickerValueRef.current?.toISOString() ?? null;

        if (currentIso !== pickedIso) {
            triggerChange(lastPickerValueRef.current);
        }
    };

    const handleOpen = () => {
        lastInputWasTypingRef.current = false;
    };

    const handleInputChange = () => {
        lastInputWasTypingRef.current = true;
    };

    const handleKeyDown = (event: React.KeyboardEvent) => {
        if (event.key.length === 1 || event.key === 'Backspace' || event.key === 'Delete') {
            lastInputWasTypingRef.current = true;
        }
    };

    const slotProps = {
        textField: {
            variant: 'outlined',
            error: error != null,
            helperText: helper,
            autoComplete: autocomplete,
            InputLabelProps: {
                title: computedLabel,
            },
            onInput: handleInputChange,
            onKeyDown: handleKeyDown,
            onPaste: handleInputChange,
            onBlur: handleBlur,
            InputProps: {
                startAdornment: startIcon && (
                    <InputAdornment position="start">{startIcon}</InputAdornment>
                ),
                endAdornment: endAction && (
                    <InputAdornment position="end">
                        {Array.isArray(endAction)
                            ? endAction.map(renderIconButton)
                            : renderIconButton(endAction)}
                    </InputAdornment>
                ),
            }
        },
        actionBar: {
            actions: ['accept', 'cancel', 'clear'],
        },
    };

    return (
        <LocalizationProvider
            dateAdapter={AdapterDateFns}
            adapterLocale={deLocale}
        >
            <DatePicker
                label={computedLabel}

                minDate={minDate}
                maxDate={maxDate}

                views={views}
                openTo={opensTo}
                // @ts-ignore
                format={format}
                value={localValue}

                onOpen={handleOpen}
                onChange={handleChange}
                onAccept={handleAccept}
                onClose={handleClose}

                disabled={disabled}

                // @ts-ignore
                slotProps={slotProps}
                sx={{
                    ...sx,
                    '& .MuiPickersInputBase-root': {
                        backgroundColor: (busy || disabled) ? '#F8F8F8' : undefined,
                        cursor: (busy || disabled) ? 'not-allowed' : undefined,
                    },
                }}
                readOnly={busy}
            />
        </LocalizationProvider>
    );
}

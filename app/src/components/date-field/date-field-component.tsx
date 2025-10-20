import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DatePicker, LocalizationProvider} from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { de } from 'date-fns/locale/de';
import {DateFieldComponentProps} from "./date-field-component-props";
import {useEffect, useMemo, useRef, useState} from 'react';
import type { Locale } from 'date-fns';
const deLocale = de as unknown as Locale;

const formatMap = {
    [DateFieldComponentModelMode.Day]: 'dd.MM.yyyy',
    [DateFieldComponentModelMode.Month]: 'MM.yyyy',
    [DateFieldComponentModelMode.Year]: 'yyyy',
};

const formatReadableMap = {
    [DateFieldComponentModelMode.Day]: 'TT.MM.JJJJ',
    [DateFieldComponentModelMode.Month]: 'MM.JJJJ',
    [DateFieldComponentModelMode.Year]: 'JJJJ',
};

const viewsMap: {
    [k in DateFieldComponentModelMode]: ('day' | 'month' | 'year')[];
} = {
    [DateFieldComponentModelMode.Day]: ['day', 'month', 'year'],
    [DateFieldComponentModelMode.Month]: ['month', 'year'],
    [DateFieldComponentModelMode.Year]: ['year'],
};

export function DateFieldComponent({
                                       label,
                                       error,
                                       hint,
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
                                   }: DateFieldComponentProps) {
    const dateValue = value != null ? new Date(value) : null;
    const [localValue, setLocalValue] = useState<Date | null>(dateValue);
    const [lastInputWasTyping, setLastInputWasTyping] = useState(false);
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const lastPickerValueRef = useRef<Date | null>(dateValue);

    useEffect(() => {
        const parsed = value ? new Date(value) : null;
        setLocalValue(parsed);
        lastPickerValueRef.current = parsed;
    }, [value]);

    const computedLabel = useMemo(() => {
        let computedLabel = label;
        if (computedLabel) {
            computedLabel += ` (${formatReadableMap[mode ?? DateFieldComponentModelMode.Day]})`;
        }
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
    const helper = useMemo(() => error != null ? error : hint, [error, hint]);

    const triggerChange = (date: Date | null) => {
        if (date === null) {
            onChange(undefined);
            onBlur?.(undefined);
            return;
        }

        if (date instanceof Date && !isNaN(date.getTime())) {
            const iso = date.toISOString();
            onChange(iso);
            onBlur?.(iso);
        }
    };

    const handleChange = (newDate: Date | null) => {
        setLocalValue(newDate);
        lastPickerValueRef.current = newDate;

        if (!lastInputWasTyping) {
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
        if (lastInputWasTyping && bufferInputUntilBlur) {
            triggerChange(localValue);
        }
    };

    const handleClose = () => {
        if (lastInputWasTyping) return;

        const currentIso = value ?? null;
        const pickedIso = lastPickerValueRef.current?.toISOString() ?? null;

        if (currentIso !== pickedIso) {
            triggerChange(lastPickerValueRef.current);
        }
    };

    const handleOpen = () => {
        setLastInputWasTyping(false);
    };

    const slotProps = useMemo(() => ({
        textField: {
            variant: 'outlined',
            error: error != null,
            helperText: helper,
            autoComplete: autocomplete,
            InputLabelProps: {
                title: computedLabel
            },
            onInput: () => setLastInputWasTyping(true),
            onBlur: handleBlur,
        },
        actionBar: {
            actions: ['accept', 'cancel', 'clear'],
        },
    }), [error, autocomplete, computedLabel, helper]);

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
                    "& .MuiPickersInputBase-root": {
                        backgroundColor: (busy || disabled) ? "#F8F8F8" : undefined,
                        cursor: (busy || disabled) ? "not-allowed" : undefined,
                    },
                }}
                readOnly={busy}
            />
        </LocalizationProvider>
    );
}


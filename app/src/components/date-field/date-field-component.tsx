import {InputAdornment} from '@mui/material';
import {DatePicker, LocalizationProvider} from '@mui/x-date-pickers';
import {DateTime} from 'luxon';
import React, {useEffect, useRef, useState} from 'react';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {ProsunaAdapterLuxon} from '../../utils/prosuna-adapter-luxon';
import {
    CalendarDatePrecision,
    dateTimeToDateValueIso,
    dateValueToDateTime,
} from '../../utils/temporal-utils';
import {DateValueIso} from '../../utils/temporal-types';
import {renderIconButton} from '../text-field/text-field-component';
import {DateFieldComponentProps} from './date-field-component-props';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {FormField, type FormFieldControlContext} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

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
                                       controlSx,
                                       bufferInputUntilBlur,
                                       debounce,
                                       startIcon,
                                       endAction,
                                       id,
                                       ariaLabel,
                                       ariaDescribedBy,
                                       labelAction,
                                       margin = 'normal',
                                       sx,
                                       size = 'small',
                                       showOptionalIndicator,
                                   }: DateFieldComponentProps) {
    const precision = mode as CalendarDatePrecision;
    const dateValue = value != null ? dateValueToDateTime(value, precision) : null;
    const [localValue, setLocalValue] = useState<DateTime | null>(dateValue);
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // MUI uses the same change callback for direct text edits and popover selection.
    // Text edits follow debounce/blur settings; popover selections are committed on accept.
    const lastInputWasTypingRef = useRef(false);

    useEffect(() => {
        const parsed = value != null ? dateValueToDateTime(value, precision) : null;
        setLocalValue(parsed);
    }, [precision, value]);

    useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const format = formatMap[mode];
    const views = viewsMap[mode];

    const triggerChange = (date: DateTime | null) => {
        if (date === null) {
            onChange(null);
            onBlur?.(null);
            return;
        }

        const dateIso = dateTimeToDateValueIso(date, precision);
        if (dateIso !== null) {
            onChange(dateIso);
            onBlur?.(dateIso);
        }
    };

    const handleChange = (newDate: DateTime | null) => {
        setLocalValue(newDate);

        if (!lastInputWasTypingRef.current || bufferInputUntilBlur) {
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

    const handlePickerChange = (newDate: unknown) => {
        if (newDate === null || DateTime.isDateTime(newDate)) {
            handleChange(newDate);
        }
    };

    const handleBlur = () => {
        if (lastInputWasTypingRef.current && bufferInputUntilBlur) {
            triggerChange(localValue);
        }
    };

    const handleAccept = (acceptedDate: unknown) => {
        if (lastInputWasTypingRef.current) {
            return;
        }

        const pickedDate = acceptedDate === null || DateTime.isDateTime(acceptedDate)
            ? acceptedDate
            : null;
        const currentIso = value != null
            ? dateTimeToDateValueIso(dateValueToDateTime(value, precision) ?? DateTime.invalid('invalid'), precision)
            : null;
        const pickedIso = pickedDate != null
            ? dateTimeToDateValueIso(pickedDate, precision)
            : null;

        if (currentIso !== pickedIso) {
            triggerChange(pickedDate);
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

    return (
        <FormField
            id={id}
            label={label}
            ariaLabel={ariaLabel}
            ariaDescribedBy={ariaDescribedBy}
            labelAction={labelAction}
            hint={hint}
            error={error}
            hideHelperText={hideHelperText}
            required={required}
            disabled={disabled}
            readOnly={busy}
            busy={busy}
            margin={margin}
            showOptionalIndicator={showOptionalIndicator}
            sx={sx}
        >
            {(fieldContext: FormFieldControlContext) => (
                <LocalizationProvider
                    dateAdapter={ProsunaAdapterLuxon}
                    adapterLocale="de"
                >
                    <DatePicker
                        label={undefined}
                        // Calendar dates are zone-free. UTC is only a stable MUI carrier that
                        // prevents the browser timezone from moving the selected calendar day.
                        timezone="UTC"
                        minDate={minDate != null ? dateValueToDateTime(minDate, 'day') ?? undefined : undefined}
                        maxDate={maxDate != null ? dateValueToDateTime(maxDate, 'day') ?? undefined : undefined}
                        views={views}
                        openTo={mode}
                        format={format}
                        value={localValue}
                        onOpen={handleOpen}
                        onChange={handlePickerChange}
                        onAccept={handleAccept}
                        disabled={disabled}
                        readOnly={busy}
                        slotProps={{
                            textField: {
                                id: fieldContext.controlId,
                                variant: 'outlined',
                                fullWidth: true,
                                margin: 'none',
                                size,
                                required,
                                error: fieldContext.invalid,
                                helperText: undefined,
                                onInput: handleInputChange,
                                onKeyDown: handleKeyDown,
                                onPaste: handleInputChange,
                                onBlur: handleBlur,
                                slotProps: {
                                    input: {
                                        ...fieldContext.ariaProps,
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
                                    },
                                    htmlInput: {
                                        autoComplete: autocomplete,
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
                                    backgroundColor: (busy || disabled) ? getDisabledFieldBackground : undefined,
                                    cursor: (busy || disabled) ? 'not-allowed' : undefined,
                                },
                            },
                            ...(Array.isArray(controlSx) ? controlSx : [controlSx]),
                        ]}
                    />
                </LocalizationProvider>
            )}
        </FormField>
    );
}

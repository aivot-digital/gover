import {InputAdornment, TextField} from '@mui/material';
import {ChangeEvent, useMemo, useRef, useState} from 'react';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {type NumberFieldComponentProps} from './number-field-component-props';
import {parseGermanNumber} from '../../utils/parse-german-numbers';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';

const AbsoluteMaxValue = Math.pow(2, 31);
const AbsoluteMinValue = -AbsoluteMaxValue;

function validateValue(inputValue: string | undefined, value: number | undefined, minValue: number | undefined, maxValue: number | undefined, decimalPlaces: number | undefined) {
    const parsedValue = inputValue == null ? (value ?? 0) : parseGermanNumber(inputValue);
    const tooLow = !isNaN(parsedValue) && parsedValue < (minValue ?? AbsoluteMinValue);
    const tooHigh = !isNaN(parsedValue) && parsedValue > (maxValue ?? AbsoluteMaxValue);
    const isEmpty = isStringNullOrEmpty(inputValue);
    const internalError = !isEmpty && isNaN(parsedValue)
        ? 'Bitte geben Sie eine gültige Zahl ein.'
        : tooLow
            ? `Der Wert muss mindestens ${formatNumToGermanNum(minValue ?? AbsoluteMinValue, decimalPlaces)} betragen.`
            : tooHigh
                ? `Der Wert darf maximal ${formatNumToGermanNum(maxValue ?? AbsoluteMaxValue, decimalPlaces)} betragen.`
                : undefined;
    return internalError;
}

export function NumberFieldComponent({
                                         label,
                                         placeholder,
                                         decimalPlaces = 0,
                                         hint,
                                         error,
                                         suffix,
                                         required,
                                         disabled,
                                         readOnly,
                                         value, // This is the original value which is passed to the component from the parent.
                                         onChange,
                                         onBlur,
                                         minValue,
                                         maxValue,
                                         bufferInputUntilBlur,
                                         debounce,
                                         sx,
                                     }: NumberFieldComponentProps) {
    // The currently inputted value in the text field. If this is not set, the original value is used.
    const [inputValue, setInputValue] = useState<string>();

    // The timeout reference for the debounce functionality.
    const debounceTimeoutRef = useRef<NodeJS.Timeout>();

    // The german string representation of the original value.
    const formattedOriginalValue = useMemo(() => {
        if (value != null) {
            return formatNumToGermanNum(value, decimalPlaces);
        }
        return '';
    }, [value, decimalPlaces]);

    // Handle user input. Check the pattern of the input.
    // This starts the debounce timeout if it is set or triggers the change otherwise.
    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        const newValue = event.target.value;

        const allowedPattern = /^-?[0-9.,]*$/;
        if (!allowedPattern.test(newValue)) {
            return; // block invalid input
        }

        setInputValue(newValue);

        if (bufferInputUntilBlur) {
            return;
        }

        if (debounce) {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
            debounceTimeoutRef.current = setTimeout(() => {
                triggerChange(newValue);
            }, debounce);
        } else {
            triggerChange(newValue);
        }
    };

    // Trigger the onChange event with the parsed value.
    const triggerChange = (val: string, formatAfter = false) => {
        const parsed = parseGermanNumber(val);
        if (!isNaN(parsed)) {
            const fixed = parseFloat(parsed.toFixed(decimalPlaces));
            onChange(fixed);

            if (formatAfter) {
                setInputValue(formatNumToGermanNum(fixed, decimalPlaces));
            }
        } else {
            onChange(undefined);
            if (formatAfter) {
                setInputValue('');
            }
        }
    };

    // Handle blur event (format value and trigger onChange)
    const handleBlur = () => {
        if (debounceTimeoutRef.current) {
            clearTimeout(debounceTimeoutRef.current);
        }

        if (inputValue == null) {
            return;
        }

        triggerChange(inputValue, true);

        if (onBlur) {
            const parsed = parseGermanNumber(inputValue);
            const finalValue = !isNaN(parsed) ? parseFloat(parsed.toFixed(decimalPlaces)) : undefined;
            onBlur(finalValue);
        }
    };

    // TODO: refactor into utility function
    const internalError = validateValue(inputValue, value, minValue, maxValue, decimalPlaces);

    return (
        <TextField
            label={label + (required ? ' *' : '')}
            placeholder={placeholder}
            variant="outlined"
            fullWidth
            InputProps={{
                endAdornment: suffix ? <InputAdornment position="end">{suffix}</InputAdornment> : undefined,
                inputProps: {style: {textAlign: 'right'}},
                sx: sx,
                readOnly: readOnly,
                'aria-disabled': readOnly || disabled,
            }}
            error={!!error || !!internalError}
            helperText={error ?? internalError ?? hint}
            value={inputValue ?? formattedOriginalValue}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled ?? false}
        />
    );
}

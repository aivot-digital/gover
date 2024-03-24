import {InputAdornment, TextField} from '@mui/material';
import {useCallback, useState} from 'react';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {type NumberFieldComponentProps} from './number-field-component-props';

export function NumberFieldComponent({
    label,
    placeholder,
    decimalPlaces,
    hint,
    error,
    suffix,
    required,
    disabled,
    value,
    onChange,
}: NumberFieldComponentProps) {
    const [valueBuffer, setValueBuffer] = useState<string>();
    const [valueBufferIsNan, setValueBufferIsNan] = useState(false);
    const [isDirty, setIsDirty] = useState(false);

    const handleChange = useCallback((event) => {
        setValueBuffer(event.target.value);
        setValueBufferIsNan(false);
        setIsDirty(true);
    }, []);

    const dec = decimalPlaces ?? 0;

    const handleBlur = (): void => {
        if (valueBuffer != null && valueBuffer !== '') {
            let num = parseFloat(valueBuffer.replaceAll('.', '').replaceAll(',', '.'));
            num = parseFloat(num.toFixed(dec));
            if (!isNaN(num)) {
                onChange(num);
                setValueBuffer(formatNumToGermanNum(num, dec));
                setValueBufferIsNan(false);
            } else {
                onChange(undefined);
                setValueBuffer('');
                setValueBufferIsNan(true);
            }
        } else {
            if (value != null && isDirty) {
                onChange(undefined);
                setValueBuffer('');
                setValueBufferIsNan(false);
            }
        }
    };

    return (
        <TextField
            label={label + (required ? ' *' : '')}
            placeholder={placeholder}
            variant="outlined"
            fullWidth
            InputProps={{
                endAdornment: suffix != null ?
                    (
                        <InputAdornment position="end">
                            {suffix}
                        </InputAdornment>
                    ) :
                    undefined,
                inputProps: {
                    style: {
                        textAlign: 'right',
                    },
                },
            }}
            error={error != null || valueBufferIsNan}
            helperText={error != null ? error : (valueBufferIsNan ? 'Bitte geben Sie eine gültige Zahl ein.' : hint)}
            value={valueBuffer == null && value != null ? formatNumToGermanNum(value, dec) : (valueBuffer ?? '')}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled ?? false}
        />
    );
}

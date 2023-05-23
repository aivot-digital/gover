import {InputAdornment, TextField} from '@mui/material';
import {useCallback, useState} from 'react';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {NumberFieldComponentProps} from "./number-field-component-props";

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

    const handleChange = useCallback(event => {
        setValueBuffer(event.target.value);
        setValueBufferIsNan(false);
    }, []);

    const handleBlur = () => {
        if (valueBuffer != null && valueBuffer !== '') {
            let num = parseFloat(valueBuffer.replaceAll('.', '').replaceAll(',', '.'));
            if (decimalPlaces != null) {
                num = parseFloat(num.toFixed(decimalPlaces));
            }
            if (!isNaN(num)) {
                onChange(num);
                setValueBuffer(formatNumToGermanNum(num, decimalPlaces));
                setValueBufferIsNan(false);
            } else {
                onChange(undefined);
                setValueBuffer('');
                setValueBufferIsNan(true);
            }
        } else {
            onChange(undefined);
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
                    ) : undefined,
                inputProps: {
                    style: {
                        textAlign: 'right',
                    },
                }
            }}
            error={error != null || valueBufferIsNan}
            helperText={error != null ? error : (valueBufferIsNan ? 'Bitte geben Sie eine gültige Zahl ein.' : hint)}
            value={valueBuffer == null && value != null ? formatNumToGermanNum(value, decimalPlaces) : (valueBuffer ?? '')}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled ?? false}
        />
    );
}

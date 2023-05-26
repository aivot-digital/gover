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

    console.log(value);

    const handleChange = useCallback(event => {
        setValueBuffer(event.target.value);
        setValueBufferIsNan(false);
    }, []);

    const dec = decimalPlaces ?? 0;

    const handleBlur = () => {
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
            value={valueBuffer == null && value != null ? formatNumToGermanNum(value, dec) : (valueBuffer ?? '')}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled ?? false}
        />
    );
}

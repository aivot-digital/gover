import {InputAdornment, TextField} from '@mui/material';
import {NumberFieldElement} from '../../models/elements/form-elements/input-elements/number-field-element';
import {useCallback, useEffect, useState} from 'react';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {BaseViewProps} from '../_lib/base-view-props';

export function NumberFieldComponentView({element, value, error, setValue}: BaseViewProps<NumberFieldElement, number>) {
    const [valueBuffer, setValueBuffer] = useState<string>();
    const [valueBufferIsNan, setValueBufferIsNan] = useState(false);

    useEffect(() => {
        if (element.id != null && element.value != null && value !== element.value) {
            setValue(element.value);
        }
    }, [value, element.id, element.value, setValue]);

    const handleChange = useCallback(event => {
        setValueBuffer(event.target.value);
        setValueBufferIsNan(false);
    }, []);

    const handleBlur = useCallback(() => {
        if (element.id != null) {
            if (valueBuffer != null && valueBuffer !== '') {
                const num = parseFloat(valueBuffer.replaceAll('.', '').replaceAll(',', '.'));
                if (!isNaN(num)) {
                    setValue(num);
                    setValueBuffer(formatNumToGermanNum(num, element.decimalPlaces));
                    setValueBufferIsNan(false);
                } else {
                    setValue(null);
                    setValueBuffer('');
                    setValueBufferIsNan(true);
                }
            } else {
                setValue(undefined);
            }
        }
    }, [element.id, element.decimalPlaces, valueBuffer, setValue]);

    return (
        <TextField
            label={element.label != null ? (element.label + (element.required ? ' *' : '')) : undefined}
            placeholder={element.placeholder}
            variant="outlined"
            fullWidth
            InputProps={{
                endAdornment: element.suffix != null ?
                    (
                        <InputAdornment position="end">
                            {element.suffix}
                        </InputAdornment>
                    ) : undefined,
                inputProps: {
                    style: {
                        textAlign: 'right',
                    },
                }
            }}
            error={error != null || valueBufferIsNan}
            helperText={error != null ? error : (valueBufferIsNan ? 'Bitte geben Sie eine gültige Zahl ein.' : element.hint)}
            value={valueBuffer == null && value != null ? formatNumToGermanNum(value, element.decimalPlaces) : (valueBuffer ?? '')}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={element.disabled ?? false}
        />
    );
}

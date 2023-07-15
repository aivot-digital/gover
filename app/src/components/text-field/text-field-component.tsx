import { Box, TextField } from '@mui/material';
import { TextFieldComponentProps } from './text-field-component-props';

export function TextFieldComponent({
                                       label,
                                       placeholder,
                                       required,
                                       disabled,
                                       multiline,
                                       value,
                                       error,
                                       hint,
                                       maxCharacters,
                                       minCharacters,
                                       onChange,
                                       onBlur,
                                       rows,
                                       type,
                                   }: TextFieldComponentProps) {
    return (
        <TextField
            label={ label }
            type={ type }
            placeholder={ placeholder }
            variant="outlined"
            fullWidth
            error={ error != null }
            multiline={ multiline }
            rows={ multiline ? (rows ?? 4) : undefined }
            FormHelperTextProps={ {
                // @ts-ignore
                component: 'div',
            } }
            helperText={
                <Box
                    sx={ {
                        display: 'flex',
                        justifyContent: 'space-between',
                    } }
                >
                    <Box>
                        { error != null ? error : hint }
                    </Box>

                    {
                        maxCharacters != null &&
                        maxCharacters > 0 &&
                        (
                            minCharacters == null ||
                            minCharacters === 0 ||
                            (value ?? '').length >= minCharacters
                        ) &&
                        <Box sx={ {ml: 3} }>
                            { (value ?? '').length }/{ maxCharacters }
                        </Box>
                    }

                    {
                        minCharacters != null &&
                        minCharacters > 0 &&
                        (value ?? '').length < minCharacters &&
                        <Box sx={ {ml: 3} }>
                            Noch mindestens { minCharacters - (value?.length ?? 0) } Zeichen
                        </Box>
                    }
                </Box>
            }
            value={ value ?? '' }
            onChange={ event => {
                const val = event.target.value;
                onChange(val.length === 0 ? undefined : val);
            } }
            onBlur={ () => {
                if (value != null) {
                    const trimmedValue = value.trim();
                    const blurValue = trimmedValue.length === 0 ? undefined : trimmedValue;
                    onChange(blurValue);
                    if (onBlur != null) {
                        onBlur(blurValue);
                    }
                }
            } }
            inputProps={
                maxCharacters ? {
                    maxLength: maxCharacters,
                } : undefined
            }
            disabled={ disabled }
            required={ required }
        />
    );
}

import {Box, TextField} from '@mui/material';
import {TextFieldComponentProps} from "./text-field-component-props";

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
                                       onChange,
                                       onBlur,
                                   }: TextFieldComponentProps) {
    return (
        <TextField
            label={label}
            placeholder={placeholder}
            variant="outlined"
            fullWidth
            error={error != null}
            multiline={multiline}
            rows={multiline ? 4 : undefined}
            FormHelperTextProps={{
                // @ts-ignore
                component: 'div',
            }}
            helperText={
                <Box
                    sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                    }}
                >
                    <Box>
                        {error != null ? error : hint}
                    </Box>
                    {
                        maxCharacters != null && maxCharacters > 0 ?
                            <Box sx={{ml: 3}}>
                                {(value ?? '').length}/{maxCharacters}
                            </Box>
                            :
                            <span/>
                    }
                </Box>
            }
            value={value ?? ''}
            onChange={event => {
                const val = event.target.value;
                onChange(val.length === 0 ? undefined : val);
            }}
            onBlur={() => {
                if (value != null) {
                    const trimmedValue = value.trim();
                    const blurValue = trimmedValue.length === 0 ? undefined : trimmedValue;
                    onChange(blurValue);
                    if (onBlur != null) {
                        onBlur(blurValue);
                    }
                }
            }}
            inputProps={
                maxCharacters ? {
                    maxLength: maxCharacters
                } : undefined
            }
            disabled={disabled}
            required={required}
        />
    );
}

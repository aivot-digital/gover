import {Box, TextField} from '@mui/material';
import {TextFieldElement} from '../../models/elements/form/input/text-field-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function TextFieldComponentView({element, value, error, setValue}: BaseViewProps<TextFieldElement, string>) {
    return (
        <TextField
            label={element.label != null ? (element.label + (element.required ? ' *' : '')) : undefined}
            placeholder={element.placeholder ?? ''}
            variant="outlined"
            fullWidth
            error={error != null}
            multiline={element.isMultiline ?? false}
            rows={element.isMultiline ? 4 : undefined}
            FormHelperTextProps={{
                // @ts-ignore
                component: 'div',
            }}
            helperText={
                <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                    <Box>
                        {error != null ? error : element.hint}
                    </Box>
                    {
                        element.maxCharacters && element.maxCharacters > 0 ?
                            <Box sx={{ml: 3}}>
                                {(value ?? '').length}/{element.maxCharacters}
                            </Box>
                            :
                            <span/>
                    }
                </Box>
            }
            value={value ?? ''}
            onChange={event => {
                if (element.id != null) {
                    const val = event.target.value;
                    if (val.length === 0) {
                        setValue(undefined);
                    } else {
                        setValue(val);
                    }
                }
            }}
            onBlur={() => {
                if (element.id != null && value != null) {
                    const trimmedValue = value.trim();
                    if (trimmedValue.length === 0) {
                        setValue(undefined);
                    } else {
                        setValue(trimmedValue);
                    }
                }
            }}
            inputProps={element.maxCharacters ? {maxLength: element.maxCharacters} : undefined}
            disabled={element.disabled ?? false}
        />
    );
}

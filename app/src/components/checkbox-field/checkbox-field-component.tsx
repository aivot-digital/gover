import {Checkbox, FormControl, FormControlLabel, FormHelperText} from '@mui/material';
import {CheckboxFieldComponentProps} from "./checkbox-field-component-props";

export function CheckboxFieldComponent({
                                           label,
                                           error,
                                           hint,
                                           required,
                                           disabled,
                                           value,
                                           onChange,
                                       }: CheckboxFieldComponentProps) {
    return (
        <FormControl
            error={error != null}
            disabled={disabled}
        >
            <FormControlLabel
                control={
                    <Checkbox
                        checked={value ?? false}
                        onChange={event => {
                            onChange(event.target.checked);
                        }}
                        disabled={disabled}
                    />
                }
                label={label + (required ? ' *' : '')}
            />
            {
                (hint != null || error != null) &&
                <FormHelperText
                    sx={{
                        ml: 0,
                    }}
                >
                    {
                        <span>{error ?? hint}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}

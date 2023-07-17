import {FormControl, FormControlLabel, FormHelperText, FormLabel, Radio, RadioGroup} from '@mui/material';
import {isStringNullOrEmpty} from "../../utils/string-utils";

export interface RadioFieldComponentProps {
    label: string;
    value?: string;
    onChange: (val: string | undefined) => void;
    options: string[];
    error?: string;
    hint?: string;
    disabled?: boolean;
    required?: boolean;
}

export function RadioFieldComponent({
                                        label,
                                        value,
                                        onChange,
                                        options,
                                        error,
                                        hint,
                                        disabled,
                                        required,
                                    }: RadioFieldComponentProps) {
    return (
        <FormControl
            component="fieldset"
            error={error != null}
            disabled={disabled}
        >
            <FormLabel
                component="legend"
            >
                {label} {required && ' *'}
            </FormLabel>
            <RadioGroup
                name="radio-buttons-group"
                value={value ?? ''}
                onChange={event => {
                    if (isStringNullOrEmpty(event.target.value)) {
                        onChange(undefined);
                    } else {
                        onChange(event.target.value ?? '');
                    }
                }}
            >
                {
                    !required &&
                    <FormControlLabel
                        value={''}
                        control={<Radio/>}
                        label="Keine Auswahl"
                        disabled={disabled}
                        sx={{fontStyle: 'italic'}}
                    />
                }
                {
                    (options ?? []).map(option => (
                        <FormControlLabel
                            key={option}
                            value={option}
                            control={<Radio/>}
                            label={option}
                            disabled={disabled}
                        />
                    ))
                }
            </RadioGroup>
            {
                (hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        hint != null &&
                        error == null &&
                        <span>{hint}</span>
                    }
                    {
                        error != null &&
                        <span>{error}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}

import {FormControl, FormControlLabel, FormHelperText, FormLabel, Radio, RadioGroup} from '@mui/material';
import {isStringNullOrEmpty} from "../../utils/string-utils";
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {useMemo} from "react";

export interface RadioFieldComponentProps {
    label: string;
    value?: string;
    onChange: (val: string | undefined) => void;
    options: Array<SelectFieldComponentOption | string>;
    error?: string;
    hint?: string;
    disabled?: boolean;
    required?: boolean;
}

const generateRandomId = () => {
    return 'id-' + Math.random().toString(36).substr(2, 9);
};

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

    const uniqueId = useMemo(() => generateRandomId(), []);

    return (
        <FormControl
            component="fieldset"
            error={error != null}
            disabled={disabled}
        >
            <FormLabel
                id={"label-" + uniqueId}
            >
                {label} {required && ' *'}
            </FormLabel>
            <RadioGroup
                aria-labelledby={"label-" + uniqueId}
                name={"radio-group-" + uniqueId}
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
                            key={typeof option === 'string' ? option : option.value}
                            value={typeof option === 'string' ? option : option.value}
                            control={<Radio/>}
                            label={typeof option === 'string' ? option : option.label}
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

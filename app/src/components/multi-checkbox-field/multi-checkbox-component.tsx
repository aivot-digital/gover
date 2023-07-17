import {Checkbox, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel} from '@mui/material';

export interface MultiCheckboxComponentProps {
    label: string;
    value?: string[];
    onChange: (val: string[] | undefined) => void;
    options: string[];
    error?: string;
    hint?: string;
    disabled?: boolean;
    required?: boolean;
}

export function MultiCheckboxComponent({
                                           label,
                                           value,
                                           onChange,
                                           options,
                                           error,
                                           hint,
                                           disabled,
                                           required,
                                       }: MultiCheckboxComponentProps) {

    const handleChange = (option: string) => {
        if (value == null || value.length === 0) {
            onChange([option]);
            return;
        }

        if (value.includes(option)) {
            const splicedList = value.filter(v => v !== option);
            onChange(splicedList.length > 0 ? splicedList : undefined);
            return;
        }

        onChange(options.filter(opt => value.includes(opt) || opt === option));
    };

    return (
        <FormControl
            error={error != null}
        >
            {
                label &&
                <FormLabel
                    component="legend"
                    disabled={disabled}
                >
                    {label} {required && '*'}
                </FormLabel>
            }
            <FormGroup>
                {
                    (options ?? []).map(option => (
                        <FormControlLabel
                            key={option}
                            control={
                                <Checkbox
                                    checked={(value ?? []).includes(option)}
                                    onChange={() => handleChange(option)}
                                    disabled={disabled}
                                />
                            }
                            label={option}
                        />
                    ))
                }
            </FormGroup>
            {
                (hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
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

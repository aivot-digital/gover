import {Checkbox, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel} from '@mui/material';
import {MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {BaseViewProps} from "../../views/base-view";

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
                                    onChange={() => {
                                            const list = value ?? [];

                                            if (list.includes(option)) {
                                                const updatedList = list.filter((elem: string) => elem !== option);
                                                if (updatedList.length === 0) {
                                                    onChange(undefined);
                                                } else {
                                                    onChange(updatedList);
                                                }
                                            } else {
                                                onChange([
                                                    ...list,
                                                    option,
                                                ]);
                                            }
                                    }}
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

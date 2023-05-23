import {Checkbox, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel} from '@mui/material';
import {MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {BaseViewProps} from "../../views/base-view";

export function MultiCheckboxFieldComponentView({
                                                    element,
                                                    value,
                                                    error,
                                                    setValue
                                                }: BaseViewProps<MultiCheckboxFieldElement, string[]>) {
    return (
        <FormControl
            error={error != null}
        >
            {
                element.label &&
                <FormLabel
                    component="legend"
                    disabled={element.disabled}
                >
                    {element.label} {element.required && '*'}
                </FormLabel>
            }
            <FormGroup>
                {
                    (element.options ?? []).map(option => (
                        <FormControlLabel
                            key={option}
                            control={
                                <Checkbox
                                    checked={(value ?? []).includes(option)}
                                    onChange={() => {
                                        if (element.id != null) {
                                            const list = value ?? [];

                                            if (list.includes(option)) {
                                                const updatedList = list.filter((elem: string) => elem !== option);
                                                if (updatedList.length === 0) {
                                                    setValue(undefined);
                                                } else {
                                                    setValue(updatedList);
                                                }
                                            } else {
                                                setValue([
                                                    ...list,
                                                    option,
                                                ]);
                                            }
                                        }
                                    }}
                                    disabled={element.disabled}
                                />
                            }
                            label={option}
                        />
                    ))
                }
            </FormGroup>
            {
                (element.hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        error == null && element.hint
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

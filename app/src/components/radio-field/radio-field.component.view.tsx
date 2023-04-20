import {FormControl, FormControlLabel, FormHelperText, FormLabel, Radio, RadioGroup} from '@mui/material';
import {RadioFieldElement} from '../../models/elements/./form/./input/radio-field-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function RadioFieldComponentView({element, value, error, setValue}: BaseViewProps<RadioFieldElement, string>) {
    return (
        <FormControl
            component="fieldset"
            error={error != null}
            disabled={element.disabled}
        >
            <FormLabel
                component="legend"
            >
                {element.label} {element.required && ' *'}
            </FormLabel>
            <RadioGroup
                name="radio-buttons-group"
                value={value ?? ''}
                onChange={event => {
                    if (element.id != null) {
                        setValue(event.target.value);
                    }
                }}
            >
                {
                    !element.required &&
                    <FormControlLabel
                        value={''}
                        control={<Radio/>}
                        label="Keine Auswahl"
                        disabled={element.disabled}
                        sx={{fontStyle: 'italic'}}
                    />
                }
                {
                    (element.options ?? []).map(option => (
                        <FormControlLabel
                            key={option}
                            value={option}
                            control={<Radio/>}
                            label={option}
                            disabled={element.disabled}
                        />
                    ))
                }
            </RadioGroup>
            {
                (element.hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        element.hint != null &&
                        error == null &&
                        element.hint
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

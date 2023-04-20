import {Checkbox, FormControl, FormControlLabel, FormHelperText} from '@mui/material';
import {CheckboxFieldElement} from '../../models/elements/./form/./input/checkbox-field-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function CheckboxFieldComponentView({
                                               setValue,
                                               element,
                                               error,
                                               value
                                           }: BaseViewProps<CheckboxFieldElement, boolean>) {
    return (
        <FormControl
            error={error != null}
            disabled={element.disabled}
        >
            <FormControlLabel
                control={
                    <Checkbox
                        checked={value ?? false}
                        onChange={event => {
                            setValue(event.target.checked);
                        }}
                        disabled={element.disabled}
                    />
                }
                label={(element.label ?? '') + (element.required ? ' *' : '')}
            />
            {
                error != null &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        <span>{error}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}

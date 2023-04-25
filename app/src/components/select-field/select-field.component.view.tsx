import {MenuItem, TextField} from '@mui/material';
import {SelectFieldElement} from '../../models/elements/form/input/select-field-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function SelectFieldComponentView({element, error, value, setValue}: BaseViewProps<SelectFieldElement, string>) {
    return (
        <TextField
            select
            fullWidth
            label={element.label != null ? (element.label + (element.required ? ' *' : '')) : undefined}
            error={error != null}
            helperText={error != null ? error : element.hint}
            placeholder={element.placeholder}
            value={value ?? ''}
            onChange={event => {
                if (element.id != null) {
                    setValue(event.target.value ?? '');
                }
            }}
            disabled={element.disabled ?? false}
        >
            {
                !element.required &&
                <MenuItem
                    value={''}
                >
                    <i>Keine Auswahl</i>
                </MenuItem>
            }
            {
                (element.options ?? []).map((option: string) => (
                    <MenuItem
                        key={option}
                        value={option}
                    >
                        {option}
                    </MenuItem>
                ))
            }
        </TextField>
    );
}


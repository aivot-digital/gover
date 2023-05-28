import {MenuItem, TextField} from '@mui/material';
import {isStringNullOrEmpty} from "../../utils/string-utils";
import {SelectFieldComponentProps} from "./select-field-component-props";

export function SelectFieldComponent({
                                         label,
                                         placeholder,
                                         hint,
                                         disabled,
                                         required,
                                         error,
                                         value,
                                         onChange,
                                         options,
                                     }: SelectFieldComponentProps) {
    const val = value ?? '';
    return (
        <TextField
            select
            fullWidth
            label={label + (required ? ' *' : '')}
            error={error != null}
            helperText={error != null ? error : hint}
            placeholder={placeholder}
            value={val}
            onChange={event => {
                if (isStringNullOrEmpty(event.target.value)) {
                    onChange(undefined);
                } else {
                    onChange(event.target.value);
                }
            }}
            disabled={disabled ?? false}
        >
            {
                !required &&
                <MenuItem
                    value={''}
                >
                    <i>Keine Auswahl</i>
                </MenuItem>
            }

            {
                options.length === 0 &&
                <MenuItem
                    value={''}
                >
                    <i>Keine Optionen vorhanden</i>
                </MenuItem>
            }
            {
                options
                    .map(option => (
                        <MenuItem
                            key={option.value}
                            value={option.value}
                        >
                            {option.label}
                        </MenuItem>
                    ))
            }
        </TextField>
    );
}


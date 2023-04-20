import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {NumberFieldElement} from '../../models/elements/./form/./input/number-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function NumberFieldComponentEditor(props: BaseEditorProps<NumberFieldElement>) {
    return (
        <>
            <TextField
                value={props.component.label ?? ''}
                label="Titel"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    label: event.target.value,
                })}
            />

            <TextField
                value={props.component.placeholder ?? ''}
                label="Platzhalter"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    placeholder: event.target.value,
                })}
            />

            <TextField
                value={props.component.hint ?? ''}
                label="Hinweis"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    hint: event.target.value,
                })}
            />

            <TextField
                value={props.component.suffix ?? ''}
                label="Einheit"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    suffix: event.target.value,
                })}
            />

            <TextField
                value={(props.component.decimalPlaces ?? 0).toString()}
                label="Dezimalstellen"
                fullWidth
                margin="normal"
                onChange={event => {
                    const val = parseInt(event.target.value ?? '0');
                    props.onPatch({
                        decimalPlaces: isNaN(val) ? 0 : val,
                    });
                }}
            />

            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={props.component.required ?? false}
                            onChange={event => props.onPatch({
                                required: event.target.checked,
                            })}
                        />
                    }
                    label="Pflichtangabe"
                />
            </FormControl>

            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={props.component.disabled ?? false}
                            onChange={event => props.onPatch({
                                disabled: event.target.checked,
                            })}
                        />
                    }
                    label="Eingabe deaktiviert"
                />
            </FormControl>
        </>
    );
}

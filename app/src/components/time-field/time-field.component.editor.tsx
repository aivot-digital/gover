import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {TimeFieldElement} from '../../models/elements/form-elements/input-elements/time-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function TimeFieldComponentEditor(props: BaseEditorProps<TimeFieldElement>) {
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
                value={props.component.hint ?? ''}
                label="Hinweis"
                fullWidth
                margin="normal"
                onChange={event => props.onPatch({
                    hint: event.target.value,
                })}
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

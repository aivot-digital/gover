import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {TextFieldElement} from '../../models/elements/./form/./input/text-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function TextFieldComponentEditor(props: BaseEditorProps<TextFieldElement>) {
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
                value={(props.component.maxCharacters ?? 0).toString()}
                label="Maximalanzahl an Zeichen"
                fullWidth
                margin="normal"
                helperText="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                onChange={event => {
                    const val = parseInt(event.target.value ?? '0');
                    props.onPatch({
                        maxCharacters: isNaN(val) ? 0 : val,
                    });
                }}
            />

            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={props.component.isMultiline ?? false}
                            onChange={event => props.onPatch({
                                isMultiline: event.target.checked,
                            })}
                        />
                    }
                    label="Mehrzeilige Texteingabe"
                />
            </FormControl>

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

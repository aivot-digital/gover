import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {TextFieldElement} from '../../models/elements/form/input/text-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function TextFieldComponentEditor(props: BaseEditorProps<TextFieldElement>) {
    return (
        <>
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
                value={(props.component.maxCharacters ?? '').toString()}
                label="Maximalanzahl an Zeichen"
                fullWidth
                margin="normal"
                helperText="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                onChange={event => {
                    const val = parseInt(event.target.value ?? '0');
                    props.onPatch({
                        maxCharacters: isNaN(val) ? undefined : val,
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
        </>
    );
}

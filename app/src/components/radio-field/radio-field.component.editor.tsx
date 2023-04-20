import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {RadioFieldElement} from '../../models/elements/./form/./input/radio-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {normalizeLines, splitLineInputEvent} from '../../utils/split-line-input';

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement>) {
    return (
        <>
            <TextField
                label="Titel"
                margin="normal"
                value={props.component.label ?? ''}
                onChange={event => props.onPatch({
                    label: event.target.value,
                })}
            />

            <TextField
                label="Hinweis"
                margin="normal"
                value={props.component.hint ?? ''}
                onChange={event => props.onPatch({
                    hint: event.target.value,
                })}
            />

            <TextField
                value={(props.component.options ?? []).join('\n')}
                label="Optionen"
                margin="normal"
                multiline
                rows={10}
                helperText="Bitte geben Sie pro Zeile eine Option an."
                onChange={event => props.onPatch({
                    options: splitLineInputEvent(event),
                })}
                onBlur={() => props.onPatch({
                    options: normalizeLines(props.component.options),
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

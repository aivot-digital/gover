import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {
    MultiCheckboxFieldElement
} from '../../models/elements/./form/./input/multi-checkbox-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {normalizeLines, splitLineInputEvent} from '../../utils/split-line-input';

export function MultiCheckboxFieldComponentEditor(props: BaseEditorProps<MultiCheckboxFieldElement>) {
    const minRequiredError = (
        props.component.minimumRequiredOptions != null &&
        props.component.options != null &&
        props.component.minimumRequiredOptions > props.component.options.length
    );

    return (
        <>
            <TextField
                value={props.component.label ?? ''}
                label="Titel"
                margin="normal"
                onChange={event => props.onPatch({
                    label: event.target.value,
                })}
            />

            <TextField
                value={props.component.hint ?? ''}
                label="Hinweis"
                margin="normal"
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
                                minimumRequiredOptions: event.target.checked ? 1 : undefined,
                            })}
                        />
                    }
                    label="Pflichtangabe"
                />
            </FormControl>

            {
                props.component.required &&
                <FormControl>
                    <TextField
                        value={props.component.minimumRequiredOptions?.toString() ?? ''}
                        label="Erforderliche Mindestanzahl"
                        helperText={minRequiredError ? 'Sie fordern mehr Optionen als Sie angegeben haben.' : 'Geben Sie 0 ein, um keine Mindestanzahl zu fordern.'}
                        onChange={event => {
                            if (event.target.value === '') {
                                props.onPatch({
                                    minimumRequiredOptions: undefined,
                                });
                                return;
                            }
                            let val = parseInt(event.target.value ?? '0');
                            if (isNaN(val)) {
                                val = 0;
                            }
                            props.onPatch({
                                minimumRequiredOptions: val,
                            });
                        }}
                        onBlur={() => {
                            if (props.component.minimumRequiredOptions == null || props.component.minimumRequiredOptions === 0) {
                                props.onPatch({
                                    required: false,
                                });
                            }
                        }}
                        error={minRequiredError}
                    />
                </FormControl>
            }

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

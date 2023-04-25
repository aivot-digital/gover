import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {FileUploadElement} from "../../models/elements/form/input/file-upload-element";
import {normalizeLines, splitLineInputEvent} from "../../utils/split-line-input";

export function FileUploadEditor(props: BaseEditorProps<FileUploadElement>) {
    const invalidMinMax = props.component.minFiles != null && props.component.maxFiles != null && props.component.minFiles > props.component.maxFiles;

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
                            checked={props.component.isMultifile ?? false}
                            onChange={event => props.onPatch({
                                isMultifile: event.target.checked,
                                minFiles: undefined,
                                maxFiles: undefined,
                            })}
                        />
                    }
                    label="Mehrere Anlagen zulässig"
                />
            </FormControl>

            {
                props.component.isMultifile &&
                <>
                    <TextField
                        value={(props.component.minFiles ?? 0).toString()}
                        label="Minimalzahl an Anlagen"
                        fullWidth
                        margin="normal"
                        helperText={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : 'Geben Sie 0 ein, um keine Minimalzahl zu fordern.'}
                        onChange={event => {
                            const val = parseInt(event.target.value ?? '0');
                            props.onPatch({
                                minFiles: isNaN(val) ? 0 : val,
                            });
                        }}
                        error={invalidMinMax}
                    />

                    <TextField
                        value={(props.component.maxFiles ?? 0).toString()}
                        label="Maximalanzahl an Anlagen"
                        fullWidth
                        margin="normal"
                        helperText={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : 'Geben Sie 0 ein, um keine Maximalanzahl zu fordern.'}
                        onChange={event => {
                            const val = parseInt(event.target.value ?? '0');
                            props.onPatch({
                                maxFiles: isNaN(val) ? 0 : val,
                            });
                        }}
                        error={invalidMinMax}
                    />
                </>
            }


            <TextField
                value={(props.component.extensions ?? []).join('\n')}
                label="Erlaubte Dateiendungen"
                fullWidth
                multiline
                rows={3}
                margin="normal"
                placeholder={'pdf\ndocx\ndoc'}
                onChange={event => props.onPatch({
                    extensions: splitLineInputEvent(event),
                })}
                onBlur={() => props.onPatch({
                    extensions: normalizeLines(props.component.extensions),
                })}
                helperText="Bitte geben Sie pro Zeile eine Dateiendung ohne Punkt an. Wenn Sie keine Dateiendungen angeben, können alle Dateien hochgeladen werden."
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

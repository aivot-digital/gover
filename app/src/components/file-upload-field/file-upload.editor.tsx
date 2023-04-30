import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {FileUploadElement} from "../../models/elements/form/input/file-upload-element";
import {StringListInput} from "../string-list-input/string-list-input";

export function FileUploadEditor(props: BaseEditorProps<FileUploadElement>) {
    const invalidMinMax = props.component.minFiles != null && props.component.maxFiles != null && props.component.minFiles > props.component.maxFiles;

    return (
        <>
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

            <StringListInput
                label="Erlaubte Dateiendungen"
                addLabel="Dateiendung hinzufügen"
                hint="Die Bürger:in kann nur Dateien mit diesen Endungen hochladen. Gebe Sie die Endung ohne Punkt an. Z.B. pdf."
                noItemsHint="Keine Endungen hinzugefügt. Bürger:innen können Dateien mit beliebiger Endung hochladen."
                value={props.component.extensions}
                onChange={extensions => props.onPatch({
                    extensions: extensions,
                })}
                allowEmpty={true}
            />
        </>
    );
}

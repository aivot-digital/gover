import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {FileUploadElement} from "../../models/elements/form/input/file-upload-element";
import {StringListInput} from "../string-list-input/string-list-input";
import {BaseEditor} from "../../editors/base-editor";

export const FileUploadEditor: BaseEditor<FileUploadElement> = ({element, onPatch}) => {
    const invalidMinMax = element.minFiles != null && element.maxFiles != null && element.minFiles > element.maxFiles;

    return (
        <>
            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={element.isMultifile ?? false}
                            onChange={event => onPatch({
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
                element.isMultifile &&
                <>
                    <TextField
                        value={(element.minFiles ?? 0).toString()}
                        label="Minimalzahl an Anlagen"
                        fullWidth
                        margin="normal"
                        helperText={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : 'Geben Sie 0 ein, um keine Minimalzahl zu fordern.'}
                        onChange={event => {
                            const val = parseInt(event.target.value ?? '0');
                            onPatch({
                                minFiles: isNaN(val) ? 0 : val,
                            });
                        }}
                        error={invalidMinMax}
                    />

                    <TextField
                        value={(element.maxFiles ?? 0).toString()}
                        label="Maximalanzahl an Anlagen"
                        fullWidth
                        margin="normal"
                        helperText={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : 'Geben Sie 0 ein, um keine Maximalanzahl zu fordern.'}
                        onChange={event => {
                            const val = parseInt(event.target.value ?? '0');
                            onPatch({
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
                value={element.extensions}
                onChange={extensions => onPatch({
                    extensions: extensions,
                })}
                allowEmpty={true}
            />
        </>
    );
}

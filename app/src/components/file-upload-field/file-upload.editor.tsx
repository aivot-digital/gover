import {Checkbox, FormControl, FormControlLabel} from '@mui/material';
import {FileUploadElement} from "../../models/elements/form/input/file-upload-element";
import {BaseEditor} from "../../editors/base-editor";
import {useEffect, useState} from "react";
import {SystemService} from "../../services/system-service";
import {NumberFieldComponent} from "../number-field/number-field-component";
import {MultiCheckboxComponent} from "../multi-checkbox-field/multi-checkbox-component";

export const FileUploadEditor: BaseEditor<FileUploadElement> = ({element, onPatch}) => {
    const [allowedExtensions, setAllowedExtensions] = useState<string[]>();

    useEffect(() => {
        SystemService
            .getFileExtensions()
            .then(setAllowedExtensions);
    }, []);

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
                    <NumberFieldComponent
                        value={element.minFiles}
                        label="Minimalzahl an Anlagen"
                        hint="Geben Sie 0 ein, um keine Minimalzahl zu fordern."
                        onChange={val => {
                            onPatch({
                                minFiles: val,
                            });
                        }}
                        error={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : undefined}
                    />

                    <NumberFieldComponent
                        value={element.maxFiles}
                        label="Maximalanzahl an Anlagen"
                        hint="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                        onChange={val => {
                            onPatch({
                                maxFiles: val,
                            });
                        }}
                        error={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : undefined}
                    />
                </>
            }

            <MultiCheckboxComponent
                label="Erlaubte Dateiendungen"
                value={element.extensions}
                onChange={val => {
                    onPatch({
                        extensions: val,
                    });
                }}
                hint="Die Bürger:in kann nur Dateien mit diesen Endungen hochladen."
                error={element.extensions == null || element.extensions.length === 0 ? 'Sie müssen mindestens eine erlaubte Endung auswählen' : undefined}
                options={allowedExtensions ?? []}
                required
            />
        </>
    );
}

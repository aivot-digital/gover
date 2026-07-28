import React, {useEffect, useState} from 'react';
import {Checkbox, FormControl, FormControlLabel, FormHelperText, Grid} from '@mui/material';
import {type FileUploadElement} from '../../models/elements/form/input/file-upload-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {MultiCheckboxComponent} from '../multi-checkbox-field/multi-checkbox-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {SystemApiService} from '../../modules/system/system-api-service';
import {TextFieldComponent} from '../text-field/text-field-component';

export function FileUploadEditor(props: BaseEditorProps<FileUploadElement>) {
    const {
        hasSummaryLayoutParent,
        hasReplicatingContainerParent = false,
    } = props;

    const dispatch = useAppDispatch();
    const [allowedExtensions, setAllowedExtensions] = useState<string[]>();

    useEffect(() => {
        new SystemApiService()
            .getFileExtensions()
            .then(setAllowedExtensions)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der erlaubten Dateiendungen'));
            });
    }, []);

    const invalidMinMax =
        props.element.minFiles != null && props.element.minFiles > 0 &&
        props.element.maxFiles != null && props.element.maxFiles > 0 &&
        props.element.minFiles > props.element.maxFiles;
    const requiresSubmittedFileNameIndex = props.element.isMultifile === true || hasReplicatingContainerParent;
    const submittedFileNameError = props.element.submittedFileName == null || props.element.submittedFileName.trim().length === 0 ?
        'Bitte geben Sie einen Dateinamen bei Einreichung an.' :
        undefined;
    const submittedFileNameHint = !requiresSubmittedFileNameIndex ?
        'Pflicht. Dieser Wert wird als Dateiname ohne Endung verwendet. Die Dateiendung kommt immer von der hochgeladenen Datei.' :
        hasReplicatingContainerParent ?
            'Pflicht. Die Dateiendung kommt immer von der hochgeladenen Datei. Der Index enthält die Datensatzpositionen strukturierter Listen von oben nach unten; bei mehreren Anlagen folgt die Anlagenposition. Ohne # wird der Index angehängt, zum Beispiel Geburtsurkunde-3-2.pdf. Mit # legen Sie die Position fest.' :
            'Pflicht. Die Dateiendung kommt immer von der hochgeladenen Datei. Bei mehreren Anlagen wird immer ein Index eingefügt: Ohne # wird er angehängt, zum Beispiel DATEINAME-1.pdf. Mit # legen Sie die Position fest, zum Beispiel # Nachweis wird zu 1 Nachweis.pdf.';

    if (hasSummaryLayoutParent) {
        return null;
    }

    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 4,
                }}
            >
                <FormControl>
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={props.element.isMultifile ?? false}
                                onChange={(event) => {
                                    props.onPatch({
                                        isMultifile: event.target.checked,
                                        minFiles: undefined,
                                        maxFiles: undefined,
                                    });
                                }}
                                disabled={!props.editable}
                            />
                        }
                        label="Mehrere Anlagen zulässig"
                    />
                    <FormHelperText sx={{ml: 4}}>
                        Lässt das Hochladen von mehr als nur einer Anlage zu.
                    </FormHelperText>
                </FormControl>
            </Grid>
            {
                props.element.isMultifile === true &&
                <>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 4,
                        }}
                    >
                        <NumberFieldComponent
                            value={props.element.required === true && props.element.minFiles == null ? 1 : props.element.minFiles ?? undefined}
                            label="Mindestanzahl an Anlagen"
                            hint="Geben Sie 0 ein, um keine Mindestanzahl zu fordern."
                            onChange={(val) => {
                                props.onPatch({
                                    minFiles: val,
                                });
                            }}
                            error={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : undefined}
                            disabled={!props.editable}
                        />
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 4,
                        }}
                    >
                        <NumberFieldComponent
                            value={props.element.maxFiles ?? undefined}
                            label="Maximalanzahl an Anlagen"
                            hint="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                            onChange={(val) => {
                                props.onPatch({
                                    maxFiles: val,
                                });
                            }}
                            error={invalidMinMax ? 'Mehr minimale Anlagen als maximale Anlagen' : undefined}
                            disabled={!props.editable}
                        />
                    </Grid>
                </>
            }
            <Grid size={12}>
                <TextFieldComponent
                    label="Dateiname bei Einreichung"
                    value={props.element.submittedFileName ?? undefined}
                    onChange={(val) => {
                        props.onPatch({
                            submittedFileName: val,
                        });
                    }}
                    hint={submittedFileNameHint}
                    error={submittedFileNameError}
                    required
                    disabled={!props.editable}
                />
            </Grid>
            <Grid size={12}>
                <MultiCheckboxComponent
                    label="Erlaubte Dateiendungen"
                    value={props.element.extensions ?? undefined}
                    onChange={(val) => {
                        props.onPatch({
                            extensions: val,
                        });
                    }}
                    hint="Die antragstellende Person kann nur Dateien mit diesen Endungen hochladen."
                    error={props.element.extensions == null || props.element.extensions.length === 0 ? 'Sie müssen mindestens eine erlaubte Endung auswählen' : undefined}
                    options={(allowedExtensions ?? []).map((ex) => ({
                        label: ex,
                        value: ex,
                    }))}
                    required
                    disabled={!props.editable}
                    displayInline={true}
                />
            </Grid>
        </Grid>
    );
};

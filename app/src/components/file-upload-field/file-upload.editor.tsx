import React, { useEffect, useState } from 'react';
import {Checkbox, FormControl, FormControlLabel, FormHelperText, Grid} from '@mui/material';
import {type FileUploadElement} from '../../models/elements/form/input/file-upload-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {MultiCheckboxComponent} from '../multi-checkbox-field/multi-checkbox-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {useApi} from "../../hooks/use-api";
import {useSystemApi} from "../../hooks/use-system-api";

export function FileUploadEditor(props: BaseEditorProps<FileUploadElement, ElementTreeEntity>) {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [allowedExtensions, setAllowedExtensions] = useState<string[]>();

    useEffect(() => {
        useSystemApi(api)
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

    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 4
                }}>
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
                    <FormHelperText sx={{ ml: 4 }}>
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
                            lg: 4
                        }}>
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
                            lg: 4
                        }}>
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
                    options={allowedExtensions ?? []}
                    required
                    disabled={!props.editable}
                    displayInline={true}
                />
            </Grid>
        </Grid>
    );
};

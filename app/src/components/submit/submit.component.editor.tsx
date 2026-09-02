import React from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {StringListInput} from '../string-list-input/string-list-input';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import { Grid } from '@mui/material';

export function SubmitComponentEditor(props: BaseEditorProps<SubmitStepElement>) {
    return (
        <>
            <Grid
                container
                columnSpacing={4}
                sx={{mt: 2}}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                    sx={{mb: 2}}
                >
                    <RichTextInputComponent
                        value={props.element.textPreSubmit ?? ''}
                        label="Text vor der Einreichung"
                        onChange={(val) => {
                            props.onPatch({
                                textPreSubmit: val ?? undefined,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                    sx={{mb: 2}}
                >
                    <RichTextInputComponent
                        value={props.element.textPostSubmit ?? ''}
                        label="Text nach der Einreichung"
                        onChange={(val) => {
                            props.onPatch({
                                textPostSubmit: val ?? undefined,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>

                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                    sx={{mb: 2}}
                >
                    <TextFieldComponent
                        value={props.element.textProcessingTime ?? ''}
                        label="Bearbeitungszeit"
                        hint="Ungefähre Bearbeitungszeit der zuständigen und/oder bewirtschaftenden Stelle."
                        multiline
                        rows={4}
                        onChange={(val) => {
                            props.onPatch({
                                textProcessingTime: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                    }}
                    sx={{mb: 2}}
                >
                    <StringListInput
                        value={props.element.documentsToReceive ?? undefined}
                        label="Dokumente, die ausfüllende Personen erhalten"
                        onChange={(val) => {
                            props.onPatch({
                                documentsToReceive: val,
                            });
                        }}
                        hint="Durch die zuständige und/oder bewirtschaftende Stelle auszustellende Dokumente. Bitte geben Sie pro Zeile ein Dokument an."
                        disabled={!props.editable}
                        allowEmpty
                        addLabel="Dokument hinzufügen"
                        noItemsHint="Keine Dokumente angegeben"
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                    }}
                    sx={{mb: 2}}
                >
                    <CheckboxFieldComponent
                        label="Konfetti nach der Einreichung deaktivieren"
                        value={props.element.disableConfetti ?? false}
                        onChange={(val) => {
                            props.onPatch({
                                disableConfetti: val,
                            });
                        }}
                        disabled={!props.editable}
                        hint="Wenn aktiviert, wird nach erfolgreicher Einreichung keine Konfetti-Animation angezeigt."
                    />
                </Grid>
            </Grid>
        </>
    );
}

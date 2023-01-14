import React from 'react';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {SubmitStepElement} from '../../models/elements/step-elements/submit-step-element';
import {TextField} from '@mui/material';
import {isNullOrEmpty} from "../../utils/is-null-or-empty";

export function SubmitComponentEditor(props: BaseEditorProps<SubmitStepElement>) {
    return (
        <>
            <TextField
                value={props.component.textPreSubmit ?? ''}
                label="Text vor dem Absenden des Antrages"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    textPreSubmit: event.target.value,
                })}
            />

            <TextField
                value={props.component.textPostSubmit ?? ''}
                label="Text nach dem Absenden des Antrages"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    textPostSubmit: event.target.value,
                })}
            />

            <TextField
                value={props.component.textProcessingTime ?? ''}
                label="Bearbeitungszeit des Antrages"
                margin="normal"
                helperText={'Ungefähre Bearbeitungszeit der zuständigen und/oder bewirtschaftenden Stelle.'}
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    textProcessingTime: event.target.value,
                })}
            />

            <TextField
                value={(props.component.documentsToReceive ?? []).join('\n')}
                label="Dokumente die Antragstellende erhalten"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    documentsToReceive: event.target.value.split('\n'),
                })}
                onBlur={() => props.onPatch({
                    documentsToReceive: (props.component.documentsToReceive ?? []).filter(ln => !isNullOrEmpty(ln)),
                })}
                helperText="Durch die zuständige und/oder bewirtschaftende Stelle auszustellende Dokumente. Bitte geben Sie pro Zeile ein Dokument an."
            />
        </>
    );
}

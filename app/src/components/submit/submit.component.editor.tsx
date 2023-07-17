import React from 'react';
import {SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {TextField} from '@mui/material';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {BaseEditorProps} from '../../editors/base-editor';
import {Application} from '../../models/entities/application';

export function SubmitComponentEditor(props: BaseEditorProps<SubmitStepElement, Application>) {
    return (
        <>
            <TextField
                value={props.element.textPreSubmit ?? ''}
                label="Text vor dem Absenden des Antrages"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    textPreSubmit: event.target.value,
                })}
                disabled={!props.editable}
            />

            <TextField
                value={props.element.textPostSubmit ?? ''}
                label="Text nach dem Absenden des Antrages"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    textPostSubmit: event.target.value,
                })}
                disabled={!props.editable}
            />

            <TextField
                value={props.element.textProcessingTime ?? ''}
                label="Bearbeitungszeit des Antrages"
                margin="normal"
                helperText={'Ungefähre Bearbeitungszeit der zuständigen und/oder bewirtschaftenden Stelle.'}
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    textProcessingTime: event.target.value,
                })}
                disabled={!props.editable}
            />

            <TextField
                value={(props.element.documentsToReceive ?? []).join('\n')}
                label="Dokumente die Antragstellende erhalten"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    documentsToReceive: event.target.value.split('\n'),
                })}
                onBlur={() => props.onPatch({
                    documentsToReceive: (props.element.documentsToReceive ?? []).filter(ln => !isStringNullOrEmpty(ln)),
                })}
                helperText="Durch die zuständige und/oder bewirtschaftende Stelle auszustellende Dokumente. Bitte geben Sie pro Zeile ein Dokument an."
                disabled={!props.editable}
            />
        </>
    );
}

import React from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {StringListInput} from '../string-list-input/string-list-input';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {LoadedForm} from '../../slices/app-slice';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';

export function SubmitComponentEditor(props: BaseEditorProps<SubmitStepElement, LoadedForm>) {
    return (
        <>
            <RichTextInputComponent
                value={props.element.textPreSubmit ?? ''}
                label="Text vor dem Absenden des Antrages"
                onChange={(val) => {
                    props.onPatch({
                        textPreSubmit: val ?? undefined,
                    });
                }}
                disabled={!props.editable}
            />

            <RichTextInputComponent
                value={props.element.textPostSubmit ?? ''}
                label="Text nach dem Absenden des Antrages"
                onChange={(val) => {
                    props.onPatch({
                        textPostSubmit: val ?? undefined,
                    });
                }}
                disabled={!props.editable}
            />

            <TextFieldComponent
                value={props.element.textProcessingTime ?? ''}
                label="Bearbeitungszeit des Antrages"
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

            <StringListInput
                value={props.element.documentsToReceive ?? undefined}
                label="Dokumente die antragstellende Personen erhalten"
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

            <CheckboxFieldComponent
                label="Konfetti nach dem Absenden deaktivieren"
                value={props.element.disableConfetti ?? false}
                onChange={(val) => {
                    props.onPatch({
                        disableConfetti: val,
                    });
                }}
                disabled={!props.editable}
                hint="Wenn aktiviert, wird nach erfolgreicher Antragseinreichung keine Konfetti-Animation angezeigt."
            />
        </>
    );
}

import React from 'react';
import { type ReplicatingContainerLayout } from '../../models/elements/form/layout/replicating-container-layout';
import { TextField } from '@mui/material';
import { type BaseEditorProps } from '../../editors/base-editor';
import { TextFieldComponent } from '../text-field/text-field-component';
import { NumberFieldComponent } from '../number-field/number-field-component';
import { Application } from '../../models/entities/application';
import { Preset } from '../../models/entities/preset';

export function ReplicatingContainerEditor(props: BaseEditorProps<ReplicatingContainerLayout, Application | Preset>): JSX.Element {
    const minRequiredError = (
        props.element.minimumRequiredSets != null &&
        props.element.maximumSets != null &&
        props.element.maximumSets > 0 &&
        props.element.minimumRequiredSets > props.element.maximumSets
    );

    return (
        <>
            <TextFieldComponent
                value={ props.element.headlineTemplate ?? '' }
                label="Überschrift des einzelnen Datensatzes"
                hint='Verwenden Sie "#" um die aktuelle Nummer des Datensatzes in der Überschrift einzusetzen.'
                onChange={ (val) => {
                    props.onPatch({
                        headlineTemplate: val,
                    });
                } }
                disabled={ !props.editable }
            />

            <TextFieldComponent
                value={ props.element.addLabel ?? '' }
                label='Label-Text für Aktion "Hinzufügen"'
                onChange={ (val) => {
                    props.onPatch({
                        addLabel: val,
                    });
                } }
                disabled={ !props.editable }
            />

            <TextFieldComponent
                value={ props.element.removeLabel ?? '' }
                label='Label-Text für Aktion "Löschen"'
                onChange={ (val) => {
                    props.onPatch({
                        removeLabel: val,
                    });
                } }
                disabled={ !props.editable }
            />

            {
                props.element.required &&
                <NumberFieldComponent
                    value={ props.element.minimumRequiredSets }
                    label="Mindestanzahl der hinzuzufügenden Datensätze"
                    hint="Geben Sie 0 ein, um keine Mindestanzahl zu fordern."
                    onChange={ (val) => {
                        props.onPatch({
                            minimumRequiredSets: val,
                        });
                    } }
                    error={ minRequiredError ? 'Sie fordern mehr Datensätze als Sie maximal zulassen.' : undefined }
                    disabled={ !props.editable }
                />
            }

            <NumberFieldComponent
                value={ props.element.maximumSets }
                label="Maximalanzahl der hinzuzufügenden Datensätze"
                hint="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                error={ minRequiredError ? 'Sie fordern mehr Datensätze als Sie maximal zulassen.' : undefined }
                onChange={ (val) => {
                    props.onPatch({
                        maximumSets: val,
                    });
                } }
                disabled={ !props.editable }
            />
        </>
    );
}

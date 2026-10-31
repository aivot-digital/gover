import React from 'react';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {Grid} from '@mui/material';

export function ReplicatingContainerEditor(props: BaseEditorProps<ReplicatingContainerLayout>) {
    const minRequiredError = (
        props.element.minimumRequiredSets != null &&
        props.element.maximumSets != null &&
        props.element.maximumSets > 0 &&
        props.element.minimumRequiredSets > props.element.maximumSets
    );

    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}>
                <TextFieldComponent
                    value={props.element.headlineTemplate ?? ''}
                    label="Überschrift des einzelnen Datensatzes"
                    hint='Verwenden Sie "#" um die aktuelle Nummer des Datensatzes in der Überschrift einzusetzen.'
                    onChange={(val) => {
                        props.onPatch({
                            headlineTemplate: val,
                        });
                    }}
                    disabled={!props.editable}
                />
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}/>
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}>
                <TextFieldComponent
                    value={props.element.addLabel ?? ''}
                    label="Beschriftung der Schaltfläche „Hinzufügen“"
                    onChange={(val) => {
                        props.onPatch({
                            addLabel: val,
                        });
                    }}
                    disabled={!props.editable}
                />
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}>
                <TextFieldComponent
                    value={props.element.removeLabel ?? ''}
                    label="Beschriftung der Schaltfläche „Entfernen“"
                    onChange={(val) => {
                        props.onPatch({
                            removeLabel: val,
                        });
                    }}
                    disabled={!props.editable}
                />
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}>
                <NumberFieldComponent
                    value={props.element.minimumRequiredSets ?? undefined}
                    label="Mindestanzahl der hinzuzufügenden Datensätze"
                    hint="Gilt, sobald Einträge vorhanden sind. Optionale Felder dürfen leer bleiben. 0 oder keine Angabe bedeutet keine Mindestanzahl."
                    onChange={(val) => {
                        props.onPatch({
                            minimumRequiredSets: (val ?? 0) > 0 ? val : undefined,
                        });
                    }}
                    error={minRequiredError ? 'Sie fordern mehr Datensätze als Sie maximal zulassen.' : undefined}
                    disabled={!props.editable}
                />
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}>
                <NumberFieldComponent
                    value={props.element.maximumSets ?? undefined}
                    label="Maximalanzahl der hinzuzufügenden Datensätze"
                    hint="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                    error={minRequiredError ? 'Sie fordern mehr Datensätze als Sie maximal zulassen.' : undefined}
                    onChange={(val) => {
                        props.onPatch({
                            maximumSets: val,
                        });
                    }}
                    disabled={!props.editable}
                />
            </Grid>
        </Grid>
    );
}

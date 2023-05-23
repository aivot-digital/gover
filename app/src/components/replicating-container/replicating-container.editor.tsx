import React from 'react';
import {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {TextField} from '@mui/material';
import {BaseEditorProps} from "../../editors/base-editor";

export function ReplicatingContainerEditor(props: BaseEditorProps<ReplicatingContainerLayout>) {
    const minRequiredError = (
        props.element.minimumRequiredSets != null &&
        props.element.maximumSets != null &&
        props.element.maximumSets > 0 &&
        props.element.minimumRequiredSets > props.element.maximumSets
    );

    return (
        <>
            <TextField
                value={props.element.headlineTemplate ?? ''}
                label="Überschrift des einzelnen Datensatzes"
                margin="normal"
                helperText='Verwenden Sie "#" um die aktuelle Nummer des Datensatzes in der Überschrift einzusetzen.'
                onChange={event => props.onPatch({
                    headlineTemplate: event.target.value,
                })}
            />

            <TextField
                value={props.element.addLabel ?? ''}
                label='Label-Text für Aktion "Hinzufügen"'
                margin="normal"
                onChange={event => props.onPatch({
                    addLabel: event.target.value,
                })}
            />

            <TextField
                value={props.element.removeLabel ?? ''}
                label='Label-Text für Aktion "Löschen"'
                margin="normal"
                onChange={event => props.onPatch({
                    removeLabel: event.target.value,
                })}
            />

            {
                props.element.required &&
                <TextField
                    value={props.element.minimumRequiredSets?.toString() ?? ''}
                    label="Mindestanzahl der hinzuzufügenden Datensätze"
                    margin="normal"
                    helperText={minRequiredError ? 'Sie fordern mehr Datensätze als Sie maximal zulassen.' : 'Geben Sie 0 ein, um keine Mindestanzahl zu fordern.'}
                    onChange={event => {
                        if (event.target.value === '') {
                            props.onPatch({
                                minimumRequiredSets: undefined,
                            });
                            return;
                        }
                        let val = parseInt(event.target.value ?? '0');
                        if (isNaN(val)) {
                            val = 0;
                        }
                        props.onPatch({
                            minimumRequiredSets: val,
                        });
                    }}
                    onBlur={() => {
                        props.onPatch({
                            required: props.element.minimumRequiredSets != null && props.element.minimumRequiredSets > 0,
                        });
                    }}
                    error={minRequiredError}
                />
            }

            <TextField
                value={props.element.maximumSets?.toString() ?? ''}
                label="Maximalanzahl der hinzuzufügenden Datensätze"
                margin="normal"
                helperText={minRequiredError ? 'Sie fordern mehr Datensätze als Sie maximal zulassen.' : 'Geben Sie 0 ein, um keine Maximalanzahl zu fordern.'}
                onChange={event => {
                    if (event.target.value === '') {
                        props.onPatch({
                            maximumSets: undefined,
                        });
                        return;
                    }
                    let val = parseInt(event.target.value ?? '0');
                    if (isNaN(val)) {
                        val = 0;
                    }
                    props.onPatch({
                        maximumSets: val,
                    });
                }}
                error={minRequiredError}
            />
        </>
    );
}

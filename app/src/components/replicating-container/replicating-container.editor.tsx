import React from 'react';
import {
    ReplicatingContainerLayout
} from '../../models/elements/form/layout/replicating-container-layout';
import {Checkbox, FormControl, FormControlLabel, TextField} from '@mui/material';
import {BaseEditorProps} from '../_lib/base-editor-props';

export function ReplicatingContainerEditor(props: BaseEditorProps<ReplicatingContainerLayout>) {
    const minRequiredError = (
        props.component.minimumRequiredSets != null &&
        props.component.maximumSets != null &&
        props.component.maximumSets > 0 &&
        props.component.minimumRequiredSets > props.component.maximumSets
    );

    return (
        <>
            <TextField
                value={props.component.label ?? ''}
                label="Titel"
                margin="normal"
                onChange={event => props.onPatch({
                    label: event.target.value,
                })}
            />

            <TextField
                value={props.component.hint ?? ''}
                label="Hinweis"
                margin="normal"
                onChange={event => props.onPatch({
                    hint: event.target.value,
                })}
            />

            <TextField
                value={props.component.headlineTemplate ?? ''}
                label="Überschrift des einzelnen Datensatzes"
                margin="normal"
                helperText='Verwenden Sie "#" um die aktuelle Nummer des Datensatzes in der Überschrift einzusetzen.'
                onChange={event => props.onPatch({
                    headlineTemplate: event.target.value,
                })}
            />

            <TextField
                value={props.component.addLabel ?? ''}
                label='Label-Text für Aktion "Hinzufügen"'
                margin="normal"
                onChange={event => props.onPatch({
                    addLabel: event.target.value,
                })}
            />

            <TextField
                value={props.component.removeLabel ?? ''}
                label='Label-Text für Aktion "Löschen"'
                margin="normal"
                onChange={event => props.onPatch({
                    removeLabel: event.target.value,
                })}
            />

            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={props.component.required ?? false}
                            onChange={event => props.onPatch({
                                required: event.target.checked,
                                minimumRequiredSets: event.target.checked ? 1 : 0,
                            })}
                        />
                    }
                    label="Pflichtangabe"
                />
            </FormControl>

            {
                props.component.required &&
                <TextField
                    value={props.component.minimumRequiredSets?.toString() ?? ''}
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
                            required: props.component.minimumRequiredSets != null && props.component.minimumRequiredSets > 0,
                        });
                    }}
                    error={minRequiredError}
                />
            }

            <TextField
                value={props.component.maximumSets?.toString() ?? ''}
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

            <FormControl>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={props.component.disabled ?? false}
                            onChange={event => props.onPatch({
                                disabled: event.target.checked,
                            })}
                        />
                    }
                    label="Eingabe deaktiviert"
                />
            </FormControl>
        </>
    );
}

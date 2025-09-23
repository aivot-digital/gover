import {BaseEditorProps} from './base-editor';
import {DateFieldComponentModelMode, type DateFieldElement} from '../models/elements/form/input/date-field-element';
import {type SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';
import React from 'react';

const modes: SelectFieldComponentOption[] = [
    {
        value: DateFieldComponentModelMode.Day,
        label: 'TT.MM.JJJJ',
    },
    {
        value: DateFieldComponentModelMode.Month,
        label: 'MM.JJJJ',
    },
    {
        value: DateFieldComponentModelMode.Year,
        label: 'JJJJ',
    },
];

export function DateFieldEditor(props: BaseEditorProps<DateFieldElement, ElementTreeEntity>) {
    const {
        element,
        onPatch,
        editable,
        scope,
    } = props;

    return (
        <>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <SelectFieldComponent
                        label="Datums-Format"
                        value={element.mode ?? DateFieldComponentModelMode.Day}
                        onChange={(val) => {
                            onPatch({
                                mode: val as DateFieldComponentModelMode,
                            });
                        }}
                        options={modes}
                        required
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    {
                        scope !== 'data_modelling' &&
                        <AutocompleteSelect
                            type={element.type}
                            value={element.autocomplete}
                            onChange={(val) => {
                                onPatch({
                                    autocomplete: val,
                                });
                            }}
                            editable={editable}
                        />
                    }
                </Grid>
            </Grid>
        </>
    );
};

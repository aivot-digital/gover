import {BaseEditorProps} from './base-editor';
import {DateRangeFieldElement} from '../models/elements/form/input/date-range-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import React from 'react';
import {Grid} from '@mui/material';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {DateFieldComponentModelMode} from '../models/elements/form/input/date-field-element';
import {SelectFieldComponentOption} from '../components/select-field/select-field-component-option';

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

export function DateRangeFieldEditor(props: BaseEditorProps<DateRangeFieldElement, ElementTreeEntity>) {
    const {
        element,
        onPatch,
        editable,
    } = props;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 12,
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
        </Grid>
    );
}

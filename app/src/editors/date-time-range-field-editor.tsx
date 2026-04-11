import {BaseEditorProps} from './base-editor';
import {DateTimeRangeFieldElement} from '../models/elements/form/input/date-time-range-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';

const modes: SelectFieldComponentOption[] = [
    {
        value: TimeFieldComponentModelMode.Minute,
        label: 'HH:mm',
    },
    {
        value: TimeFieldComponentModelMode.Second,
        label: 'HH:mm:ss',
    },
];

export function DateTimeRangeFieldEditor(props: BaseEditorProps<DateTimeRangeFieldElement, ElementTreeEntity>) {
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
            <Grid size={{xs: 12}}>
                <SelectFieldComponent
                    label="Uhrzeit-Format"
                    value={element.mode ?? TimeFieldComponentModelMode.Minute}
                    onChange={(val) => {
                        onPatch({
                            mode: val as TimeFieldComponentModelMode,
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

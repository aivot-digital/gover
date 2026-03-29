import {BaseEditorProps} from './base-editor';
import {TimeRangeFieldElement} from '../models/elements/form/input/time-range-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
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

export function TimeRangeFieldEditor(props: BaseEditorProps<TimeRangeFieldElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
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
            <Grid size={{xs: 12}}>
                <CheckboxFieldComponent
                    label="Offene Spanne erlauben"
                    value={element.allowOpenRange ?? false}
                    onChange={(checked) => {
                        onPatch({
                            allowOpenRange: checked,
                        });
                    }}
                    hint="Wenn aktiviert, darf nur ein Grenzwert (Von oder Bis) angegeben werden."
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}

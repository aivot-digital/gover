import {BaseEditorProps} from './base-editor';
import {DateTimeFieldElement} from '../models/elements/form/input/date-time-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
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

export function DateTimeFieldEditor(props: BaseEditorProps<DateTimeFieldElement, ElementTreeEntity>) {
    const {
        element,
        onPatch,
        editable,
    } = props;

    return (
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
    );
}

import {type BaseEditor} from './base-editor';
import {DateFieldComponentModelMode, type DateFieldElement} from '../models/elements/form/input/date-field-element';
import {type SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';

const modes: SelectFieldComponentOption[] = [
    {
        value: DateFieldComponentModelMode.Date,
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

export const DateFieldEditor: BaseEditor<DateFieldElement, ElementTreeEntity> = ({
                                                                                     element,
                                                                                     onPatch,
                                                                                     editable,
                                                                                 }) => {
    return (
        <>
            <SelectFieldComponent
                label="Datums-Format"
                value={element.mode ?? DateFieldComponentModelMode.Date}
                onChange={(val) => {
                    onPatch({
                        mode: val as DateFieldComponentModelMode,
                    });
                }}
                options={modes}
                required
                disabled={!editable}
            />
        </>
    );
};

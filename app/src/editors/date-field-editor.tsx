import {BaseEditor} from "./base-editor";
import {DateFieldComponentModelMode, DateFieldElement} from "../models/elements/form/input/date-field-element";
import {SelectFieldComponentOption} from "../components/select-field/select-field-component-option";
import {SelectFieldComponent} from "../components/select-field/select-field-component";
import {CheckboxFieldComponent} from "../components/checkbox-field/checkbox-field-component";

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

export const DateFieldEditor: BaseEditor<DateFieldElement> = ({element, onPatch}) => {
    return (
        <>
            <SelectFieldComponent
                label="Datums-Format"
                value={element.mode ?? DateFieldComponentModelMode.Date}
                onChange={val => onPatch({
                    mode: val as DateFieldComponentModelMode,
                })}
                options={modes}
                required
            />

            <CheckboxFieldComponent
                label="Das Datum muss in der Vergangenheit liegen"
                value={element.mustBePast}
                onChange={val => onPatch({
                    mustBePast: val,
                })}
                disabled={element.mustBeFuture}
                hint={element.mustBeFuture ? 'Ein Datum kann nicht gleichzeitig in der Vergangenheit und Zukunft liegen.' : undefined}
            />

            <CheckboxFieldComponent
                label="Das Datum muss in der Zukunft liegen"
                value={element.mustBeFuture}
                onChange={val => onPatch({
                    mustBeFuture: val,
                })}
                disabled={element.mustBePast}
                hint={element.mustBePast ? 'Ein Datum kann nicht gleichzeitig in der Vergangenheit und Zukunft liegen.' : undefined}
            />
        </>
    );
}

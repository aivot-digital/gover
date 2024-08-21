import {type NumberFieldElement} from '../models/elements/form/input/number-field-element';
import {type BaseEditor} from './base-editor';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';

export const NumberFieldEditor: BaseEditor<NumberFieldElement, ElementTreeEntity> = ({
                                                                                         element,
                                                                                         onPatch,
                                                                                         editable,
                                                                                     }) => {
    return (
        <>
            <TextFieldComponent
                value={element.placeholder ?? ''}
                label="Platzhalter"
                onChange={(val) => {
                    onPatch({
                        placeholder: val,
                    });
                }}
                hint={"Ein Platzhalter ist eine Musterangabe, die zeigt, welche Information eingegeben werden sollen und erwartet werden. Bei einem E-Mail-Feld wäre eine Möglichkeit z.B. „hallo@bad-musterstadt.de“."}
                disabled={!editable}
            />

            <TextFieldComponent
                value={element.suffix ?? ''}
                label="Einheit"
                onChange={(val) => {
                    onPatch({
                        suffix: val,
                    });
                }}
                disabled={!editable}
            />

            <NumberFieldComponent
                value={element.decimalPlaces}
                label="Dezimalstellen"
                onChange={(val) => {
                    onPatch({
                        decimalPlaces: val,
                    });
                }}
                disabled={!editable}
            />
        </>
    );
};

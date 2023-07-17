import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {type BaseEditor} from './base-editor';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {type Application} from '../models/entities/application';
import {type Preset} from '../models/entities/preset';

export const TextFieldEditor: BaseEditor<TextFieldElement, Application | Preset> = ({
                                                                                        element,
                                                                                        onPatch,
                                                                                        editable,
                                                                                    }) => {
    return (
        <>
            <TextFieldComponent
                value={element.placeholder}
                label="Platzhalter"
                onChange={(value) => {
                    onPatch({
                        placeholder: value,
                    });
                }}
                disabled={!editable}
            />

            <NumberFieldComponent
                label="Minimalanzahl an Zeichen"
                value={element.minCharacters}
                onChange={(val) => {
                    onPatch({
                        minCharacters: val,
                    });
                }}
                error={element.maxCharacters != null && element.minCharacters != null && element.minCharacters > element.maxCharacters ? 'Sie fordern mehr Zeichen als Sie maximal zulassen.' : undefined}
                hint="Geben Sie 0 oder nichts ein, um keine Minimalanzahl zu fordern."
                disabled={!editable}
            />

            <NumberFieldComponent
                label="Maximalanzahl an Zeichen"
                value={element.maxCharacters}
                onChange={(val) => {
                    onPatch({
                        maxCharacters: val,
                    });
                }}
                error={element.maxCharacters != null && element.minCharacters != null && element.maxCharacters < element.minCharacters ? 'Sie fordern mehr Zeichen als Sie maximal zulassen.' : undefined}
                hint="Geben Sie 0 oder nichts ein, um keine Maximalanzahl zu fordern."
                disabled={!editable}
            />

            <CheckboxFieldComponent
                label="Mehrzeilige Texteingabe"
                value={element.isMultiline}
                onChange={(checked) => {
                    onPatch({
                        isMultiline: checked,
                    });
                }}
                disabled={!editable}
            />
        </>
    );
};

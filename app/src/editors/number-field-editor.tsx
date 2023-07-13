import { type NumberFieldElement } from '../models/elements/form/input/number-field-element';
import { type BaseEditor } from './base-editor';
import { TextFieldComponent } from '../components/text-field/text-field-component';
import { NumberFieldComponent } from '../components/number-field/number-field-component';

export const NumberFieldEditor: BaseEditor<NumberFieldElement> = ({
                                                                      element,
                                                                      onPatch,
                                                                      editable,
                                                                  }) => {
    return (
        <>
            <TextFieldComponent
                value={ element.placeholder ?? '' }
                label="Platzhalter"
                onChange={ (val) => {
                    onPatch({
                        placeholder: val,
                    });
                } }
                disabled={ !editable }
            />

            <TextFieldComponent
                value={ element.suffix ?? '' }
                label="Einheit"
                onChange={ (val) => {
                    onPatch({
                        suffix: val,
                    });
                } }
                disabled={ !editable }
            />

            <NumberFieldComponent
                value={ element.decimalPlaces }
                label="Dezimalstellen"
                onChange={ (val) => {
                    onPatch({
                        decimalPlaces: val,
                    });
                } }
                disabled={ !editable }
            />
        </>
    );
};

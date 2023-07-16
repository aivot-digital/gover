import { type BaseEditor } from './base-editor';
import { type SelectFieldElement } from '../models/elements/form/input/select-field-element';
import { StringListInput } from '../components/string-list-input/string-list-input';
import { Application } from '../models/entities/application';
import { Preset } from '../models/entities/preset';

export const SelectFieldEditor: BaseEditor<SelectFieldElement, Application | Preset> = ({
                                                                      element,
                                                                      onPatch,
                                                                      editable,
                                                                  }) => {
    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={ element.options }
                onChange={ (options) => {
                    onPatch({
                        options,
                    });
                } }
                allowEmpty={ false }
                disabled={ !editable }
            />
        </>
    );
};

import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {StringListInput} from '../string-list-input/string-list-input';
import {type BaseEditorProps} from '../../editors/base-editor';
import {Application} from '../../models/entities/application';
import {Preset} from '../../models/entities/preset';

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement, Application | Preset>): JSX.Element {
    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={props.element.options}
                onChange={(options) => {
                    props.onPatch({
                        options,
                    });
                }}
                allowEmpty={false}
                disabled={!props.editable}
            />
        </>
    );
}

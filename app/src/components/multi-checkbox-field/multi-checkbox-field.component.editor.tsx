import { type MultiCheckboxFieldElement } from '../../models/elements/form/input/multi-checkbox-field-element';
import { StringListInput } from '../string-list-input/string-list-input';
import { type BaseEditorProps } from '../../editors/base-editor';
import { NumberFieldComponent } from '../number-field/number-field-component';
import { Application } from '../../models/entities/application';
import { Preset } from '../../models/entities/preset';

export function MultiCheckboxFieldComponentEditor(props: BaseEditorProps<MultiCheckboxFieldElement, Application | Preset>): JSX.Element {
    const minRequiredError = (
        props.element.minimumRequiredOptions != null &&
        props.element.options != null &&
        props.element.minimumRequiredOptions > props.element.options.length
    );

    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann eine oder mehrere dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={ props.element.options }
                onChange={ (options) => {
                    props.onPatch({
                        options,
                    });
                } }
                allowEmpty={ false }
                disabled={ !props.editable }
            />

            {
                props.element.required &&
                <NumberFieldComponent
                    label="Erforderliche Mindestanzahl"
                    value={ props.element.minimumRequiredOptions ?? 1 }
                    onChange={ (val) => {
                        props.onPatch({
                            required: val === 0 ? false : props.element.required,
                            minimumRequiredOptions: val === 0 ? undefined : val,
                        });
                    } }
                    error={ minRequiredError ? 'Sie fordern mehr Optionen als Sie definiert haben.' : undefined }
                    hint="Geben Sie 0 ein, um keine Mindestanzahl zu fordern."
                    disabled={ !props.editable }
                />
            }
        </>
    );
}

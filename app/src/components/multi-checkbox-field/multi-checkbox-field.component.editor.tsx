import {FormControl, TextField} from '@mui/material';
import {MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {StringListInput} from "../string-list-input/string-list-input";

export function MultiCheckboxFieldComponentEditor(props: BaseEditorProps<MultiCheckboxFieldElement>) {
    const minRequiredError = (
        props.component.minimumRequiredOptions != null &&
        props.component.options != null &&
        props.component.minimumRequiredOptions > props.component.options.length
    );

    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann eine oder mehrere dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={props.component.options}
                onChange={options => props.onPatch({
                    options: options,
                })}
                allowEmpty={false}
            />

            {
                props.component.required &&
                <FormControl>
                    <TextField
                        value={props.component.minimumRequiredOptions?.toString() ?? ''}
                        label="Erforderliche Mindestanzahl"
                        helperText={minRequiredError ? 'Sie fordern mehr Optionen als Sie angegeben haben.' : 'Geben Sie 0 ein, um keine Mindestanzahl zu fordern.'}
                        onChange={event => {
                            if (event.target.value === '') {
                                props.onPatch({
                                    minimumRequiredOptions: undefined,
                                });
                                return;
                            }
                            let val = parseInt(event.target.value ?? '0');
                            if (isNaN(val)) {
                                val = 0;
                            }
                            props.onPatch({
                                minimumRequiredOptions: val,
                            });
                        }}
                        onBlur={() => {
                            if (props.component.minimumRequiredOptions == null || props.component.minimumRequiredOptions === 0) {
                                props.onPatch({
                                    required: false,
                                });
                            }
                        }}
                        error={minRequiredError}
                    />
                </FormControl>
            }
        </>
    );
}

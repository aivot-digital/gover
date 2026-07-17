import React from 'react';
import {type MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {OptionListInput} from '../option-list-input/option-list-input';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {OptionsSourceType} from '../../models/elements/form/input/options-source-type';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {CodeListSelectField} from '../../modules/code-lists/components/code-list-select-field';

const optionsSourceOptions = [
    {
        label: 'Manuell konfigurieren',
        value: OptionsSourceType.Manual,
    },
    {
        label: 'Codeliste verwenden',
        value: OptionsSourceType.CodeList,
    },
];

export function MultiCheckboxFieldComponentEditor(props: BaseEditorProps<MultiCheckboxFieldElement>) {
    const optionsSource = props.element.optionsSource ?? OptionsSourceType.Manual;
    const usesManualOptions = optionsSource === OptionsSourceType.Manual;

    const minRequiredError = (
        usesManualOptions &&
        props.element.minimumRequiredOptions != null &&
        props.element.options != null &&
        props.element.minimumRequiredOptions > props.element.options.length
    );

    const options = props.element.options ?? [];

    return (
        <>
            <SelectFieldComponent
                label="Optionsquelle"
                value={optionsSource}
                onChange={(value) => {
                    const nextSource = (value as OptionsSourceType | null) ?? OptionsSourceType.Manual;

                    props.onPatch({
                        optionsSource: nextSource,
                        codeListId: nextSource === OptionsSourceType.CodeList ? props.element.codeListId : undefined,
                    });
                }}
                options={optionsSourceOptions}
                disabled={!props.editable}
                required
            />

            {
                usesManualOptions &&
                <OptionListInput
                    label="Optionen"
                    addLabel="Option hinzufügen"
                    hint="Die antragstellende Person kann eine oder mehrere dieser Optionen auswählen."
                    noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                    value={options as any}
                    onChange={(options) => {
                        props.onPatch({
                            options,
                        });
                    }}
                    allowEmpty={false}
                    disabled={!props.editable}
                    variant="outlined"
                />
            }

            {
                !usesManualOptions &&
                <CodeListSelectField
                    value={props.element.codeListId}
                    onChange={(codeListId) => {
                        props.onPatch({
                            codeListId,
                        });
                    }}
                    disabled={!props.editable}
                    required
                />
            }

            {
                props.element.required === true &&
                <NumberFieldComponent
                    label="Erforderliche Mindestanzahl"
                    value={props.element.minimumRequiredOptions ?? 1}
                    onChange={(val) => {
                        props.onPatch({
                            required: val === 0 ? false : props.element.required,
                            minimumRequiredOptions: val === 0 ? undefined : val,
                        });
                    }}
                    error={minRequiredError ? 'Sie fordern mehr Optionen als Sie definiert haben.' : undefined}
                    hint="Geben Sie 0 ein, um keine Mindestanzahl zu fordern."
                    disabled={!props.editable}
                />
            }

            {
                !props.hasSummaryLayoutParent &&
                <CheckboxFieldComponent
                    label="Optionen nebeneinander anzeigen"
                    value={props.element.displayInline ?? undefined}
                    onChange={(checked) => {
                        props.onPatch({
                            displayInline: checked,
                        });
                    }}
                    disabled={!props.editable}
                    hint="Zeigt die Optionen nebeneinander anstatt untereinander an. Dies kann Platz sparen und die Übersichtlichkeit verbessern."
                />
            }
        </>
    );
}

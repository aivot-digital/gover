import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {OptionListInput} from '../option-list-input/option-list-input';
import React from 'react';
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

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement>) {
    const optionsSource = props.element.optionsSource ?? OptionsSourceType.Manual;
    const usesManualOptions = optionsSource === OptionsSourceType.Manual;

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
                    hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
                    noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                    value={props.element.options ?? []}
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
                !props.hasSummaryLayoutParent &&
                <CheckboxFieldComponent
                    label="Optionen nebeneinander anzeigen"
                    value={props.element.displayInline ?? false}
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

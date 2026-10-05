import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {OptionListInput} from '../option-list-input/option-list-input';
import React from 'react';
import {Grid} from '@mui/material';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {OptionsSourceType} from '../../models/elements/form/input/options-source-type';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {CodeListSelectField} from '../../modules/code-lists/components/code-list-select-field';

const optionsSourceOptions = [
    {
        label: 'Manuelle Eingabe',
        value: OptionsSourceType.Manual,
    },
    {
        label: 'System-Codeliste',
        value: OptionsSourceType.CodeList,
    },
];

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement>) {
    const optionsSource = props.element.optionsSource ?? OptionsSourceType.Manual;
    const usesManualOptions = optionsSource === OptionsSourceType.Manual;

    return (
        <>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
                <Grid size={{xs: 12, lg: 6}}>
                    <SelectFieldComponent
                        label="Optionen definieren über"
                        value={optionsSource}
                        onChange={(value) => {
                            const nextSource = (value as OptionsSourceType | null) ?? OptionsSourceType.Manual;

                            props.onPatch({
                                optionsSource: nextSource,
                                codeListKey: nextSource === OptionsSourceType.CodeList ? props.element.codeListKey : undefined,
                            });
                        }}
                        options={optionsSourceOptions}
                        disabled={!props.editable}
                        required
                    />
                </Grid>

                {
                    !usesManualOptions &&
                    <Grid size={{xs: 12, lg: 6}}>
                        <CodeListSelectField
                            value={props.element.codeListKey}
                            onChange={(codeListKey) => {
                                props.onPatch({
                                    codeListKey,
                                });
                            }}
                            disabled={!props.editable}
                            required
                        />
                    </Grid>
                }
            </Grid>

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

import React, {useMemo} from 'react';
import {type BaseEditorProps} from './base-editor';
import {
    type SelectFieldElement,
    type SelectFieldElementOption,
} from '../models/elements/form/input/select-field-element';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementType} from '../data/element-type/element-type';
import {generateComponentPath, generateComponentTitle} from '../utils/generate-component-title';
import {useElementTreeContext} from '../components/element-tree-2/element-tree-context';
import {SelectFieldComponentOption} from '../components/select-field-2/select-field-component';
import Grid from '@mui/material/Grid';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {CodeListSelectField} from '../modules/code-lists/components/code-list-select-field';

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

export function SelectFieldEditor(props: BaseEditorProps<SelectFieldElement>) {
    const {
        element,
        editable,
        onPatch,
        scope,
        hasSummaryLayoutParent,
    } = props;

    const {
        allElements,
    } = useElementTreeContext();

    const optionsSource = element.optionsSource ?? OptionsSourceType.Manual;
    const usesManualOptions = optionsSource === OptionsSourceType.Manual;

    const options = useMemo(() => {
        return (element.options ?? [])
            .map((option): SelectFieldElementOption => {
                if (typeof option === 'string') {
                    return {
                        value: option,
                        label: option,
                    };
                }

                return option;
            });
    }, [element.options]);

    const dependencyOptions: SelectFieldComponentOption<string>[] = useMemo(() => {
        const currentElementIndex = allElements.findIndex(({element: candidate}) => candidate.id === element.id);
        const relevantElements = currentElementIndex >= 0 ? allElements.slice(0, currentElementIndex) : allElements;

        return relevantElements
            .filter(({element: candidate}) => candidate.type === ElementType.Select && candidate.id !== element.id)
            .map(({
                      element: candidate,
                      parents,
                  }) => ({
                label: generateComponentTitle(candidate),
                subLabel: generateComponentPath(parents),
                value: candidate.id,
            }));
    }, [element.id, allElements]);

    const hasDependency = usesManualOptions && !hasSummaryLayoutParent && element.dependsOnSelectFieldId != null;

    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <SelectFieldComponent
                    label="Optionen definieren über"
                    value={optionsSource}
                    onChange={(value) => {
                        const nextSource = (value as OptionsSourceType | null) ?? OptionsSourceType.Manual;

                        onPatch({
                            optionsSource: nextSource,
                            codeListId: nextSource === OptionsSourceType.CodeList ? element.codeListId : undefined,
                            dependsOnSelectFieldId: nextSource === OptionsSourceType.CodeList ? undefined : element.dependsOnSelectFieldId,
                        });
                    }}
                    options={optionsSourceOptions}
                    disabled={!editable}
                    required
                />
            </Grid>

            {
                usesManualOptions &&
                !hasSummaryLayoutParent &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <SelectFieldComponent
                        label="Abhängiges Auswahlfeld (optional)"
                        hint="Wählen Sie ein vorheriges Auswahlfeld aus, wenn diese Auswahl von dessen Wert abhängen soll. Dann können die Optionen über Gruppenwerte gezielt zugeordnet werden."
                        value={element.dependsOnSelectFieldId ?? undefined}
                        onChange={(dependsOnSelectFieldId) => {
                            onPatch({
                                dependsOnSelectFieldId,
                            });
                        }}
                        options={dependencyOptions}
                        disabled={!editable}
                        emptyStatePlaceholder="Keine vorherigen Auswahlfelder verfügbar"
                    />
                </Grid>
            }

            {
                !usesManualOptions &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <CodeListSelectField
                        value={element.codeListId}
                        onChange={(codeListId) => {
                            onPatch({
                                codeListId,
                            });
                        }}
                        disabled={!editable}
                        required
                    />
                </Grid>
            }

            {
                usesManualOptions &&
                <Grid
                    size={{
                        xs: 12,
                    }}
                >
                    <OptionListInput
                        label="Optionen"
                        addLabel="Option hinzufügen"
                        hint={hasDependency
                            ? 'Die antragstellende Person kann genau eine dieser Optionen auswählen. Der Gruppenwert verknüpft eine Option mit dem Wert des abhängigen Auswahlfelds; leere Gruppen bleiben weiterhin immer sichtbar.'
                            : 'Die antragstellende Person kann genau eine dieser Optionen auswählen.'}
                        noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                        value={options}
                        onChange={(options) => {
                            onPatch({
                                options,
                            });
                        }}
                        allowEmpty={false}
                        disabled={!editable}
                        variant="outlined"
                        showGroupField={hasDependency}
                        groupLabel="Gruppenwert"
                    />
                </Grid>
            }

            {
                scope !== 'data_modelling' &&
                !hasSummaryLayoutParent &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <AutocompleteSelect
                        type={element.type}
                        value={element.autocomplete}
                        onChange={(val) => {
                            onPatch({
                                autocomplete: val,
                            });
                        }}
                        editable={editable}
                    />
                </Grid>
            }
        </Grid>
    );
}

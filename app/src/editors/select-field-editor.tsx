import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
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
import {type SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import Grid from '@mui/material/Grid';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {CodeListSelectField} from '../modules/code-lists/components/code-list-select-field';
import {CodeListColumnSelectField} from '../modules/code-lists/components/code-list-column-select-field';
import {useCodeListColumnOptions} from '../modules/code-lists/hooks/use-code-list-column-options';
import {useCodeListElementOptions} from '../modules/code-lists/hooks/use-code-list-element-options';
import {type AnyElement} from '../models/elements/any-element';
import {SelectFieldPresentation} from '../models/elements/form/input/select-field-presentation';

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

const presentationOptions: SelectFieldComponentOption<SelectFieldPresentation>[] = [
    {label: 'Optionsfelder', subLabel: 'Alle Optionen sind direkt sichtbar.', value: SelectFieldPresentation.Radio},
    {
        label: 'Auswahlmenü',
        subLabel: 'Kompakte Darstellung für kurze, überschaubare Optionslisten.',
        value: SelectFieldPresentation.Dropdown,
    },
    {
        label: 'Durchsuchbare Auswahl',
        subLabel: 'Die Optionsliste kann durch Texteingabe gefiltert werden.',
        value: SelectFieldPresentation.Combobox,
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
    const presentation = element.presentation ?? SelectFieldPresentation.Dropdown;
    const usesManualOptions = optionsSource === OptionsSourceType.Manual;
    const hasDependency = !hasSummaryLayoutParent && element.dependsOnSelectFieldId != null;
    const codeListColumns = useCodeListColumnOptions(usesManualOptions ? null : element.codeListKey);

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

    const groupOptions = useReferencedSelectValues(
        usesManualOptions && hasDependency
            ? allElements.find(({element: candidate}) => candidate.id === element.dependsOnSelectFieldId)?.element
            : undefined,
    );

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
                    label="Darstellung der Auswahl"
                    value={presentation}
                    onChange={(value) => {
                        onPatch({
                            presentation: value ?? SelectFieldPresentation.Dropdown,
                        });
                    }}
                    options={presentationOptions}
                    disabled={!editable}
                    required
                />
            </Grid>

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
                        const nextSource = value ?? OptionsSourceType.Manual;

                        const usesCodeList = nextSource === OptionsSourceType.CodeList;
                        onPatch({
                            optionsSource: nextSource,
                            codeListKey: usesCodeList ? element.codeListKey : undefined,
                            codeListValueColumnKey: usesCodeList ? element.codeListValueColumnKey : undefined,
                            codeListLabelColumnKey: usesCodeList ? element.codeListLabelColumnKey : undefined,
                            codeListGroupColumnKey: usesCodeList ? element.codeListGroupColumnKey : undefined,
                        });
                    }}
                    options={optionsSourceOptions}
                    disabled={!editable}
                    required
                />
            </Grid>

            {
                !hasSummaryLayoutParent &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <SelectFieldComponent
                        label="Abhängiges Auswahlfeld (optional)"
                        hint={usesManualOptions
                            ? 'Wählen Sie ein vorheriges Auswahlfeld aus, wenn diese Auswahl von dessen Wert abhängen soll. Dann können die Optionen über Gruppenwerte gezielt zugeordnet werden.'
                            : 'Wählen Sie ein vorheriges Auswahlfeld aus, wenn diese Auswahl von dessen Wert abhängen soll. Dann werden nur Einträge angezeigt, deren Gruppenwert dem gewählten Wert entspricht.'}
                        value={element.dependsOnSelectFieldId ?? undefined}
                        onChange={(dependsOnSelectFieldId) => {
                            onPatch({
                                dependsOnSelectFieldId,
                                codeListGroupColumnKey: dependsOnSelectFieldId == null ? undefined : element.codeListGroupColumnKey,
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
                        value={element.codeListKey}
                        onChange={(codeListKey) => {
                            // Column keys belong to one code list, so they do not carry over to another one.
                            onPatch(codeListKey === element.codeListKey ? {codeListKey} : {
                                codeListKey,
                                codeListValueColumnKey: undefined,
                                codeListLabelColumnKey: undefined,
                                codeListGroupColumnKey: undefined,
                            });
                        }}
                        disabled={!editable}
                        required
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
                    <CodeListColumnSelectField
                        label="Wert-Spalte"
                        hint="Die Werte dieser Spalte werden gespeichert und an nachfolgende Prozessschritte oder Systeme übergeben."
                        value={element.codeListValueColumnKey}
                        onChange={(codeListValueColumnKey) => onPatch({codeListValueColumnKey})}
                        columnOptions={codeListColumns.options}
                        columnOptionsLoaded={codeListColumns.loaded}
                        disabled={!editable}
                        required
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
                    <CodeListColumnSelectField
                        label="Beschriftungs-Spalte"
                        hint="Die Werte dieser Spalte werden als Beschriftung der Optionen angezeigt. Einträge mit demselben Wert werden zu einer Option zusammengefasst."
                        value={element.codeListLabelColumnKey}
                        onChange={(codeListLabelColumnKey) => onPatch({codeListLabelColumnKey})}
                        columnOptions={codeListColumns.options}
                        columnOptionsLoaded={codeListColumns.loaded}
                        disabled={!editable}
                        required
                    />
                </Grid>
            }

            {
                !usesManualOptions &&
                hasDependency &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <CodeListColumnSelectField
                        label="Gruppenwert-Spalte"
                        hint="Es werden nur Einträge angezeigt, deren Wert in dieser Spalte dem gewählten Wert des abhängigen Auswahlfelds entspricht. Einträge ohne Gruppenwert werden nicht angezeigt."
                        value={element.codeListGroupColumnKey}
                        onChange={(codeListGroupColumnKey) => onPatch({codeListGroupColumnKey})}
                        columnOptions={codeListColumns.options}
                        columnOptionsLoaded={codeListColumns.loaded}
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
                        groupOptions={groupOptions}
                    />
                </Grid>
            }

            {presentation === SelectFieldPresentation.Radio && !hasSummaryLayoutParent && <Grid size={{xs: 12, lg: 6}}>
                <CheckboxFieldComponent
                    label="Optionen nebeneinander anzeigen"
                    value={element.displayInline ?? false}
                    onChange={(displayInline) => onPatch({displayInline})}
                    disabled={!editable}
                />
            </Grid>}
            {
                presentation !== SelectFieldPresentation.Radio &&
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

const NO_ELEMENTS: AnyElement[] = [];

/** Lists the values of a referenced select, which the group values of a dependent select refer to. */
function useReferencedSelectValues(referencedElement: AnyElement | undefined): Array<{value: string; label: string}> {
    const isCodeListSelect = referencedElement?.type === ElementType.Select &&
        referencedElement.optionsSource === OptionsSourceType.CodeList;
    const codeListElements = useMemo(
        () => isCodeListSelect && referencedElement != null ? [referencedElement] : NO_ELEMENTS,
        [isCodeListSelect, referencedElement],
    );
    const codeListOptions = useCodeListElementOptions(codeListElements);

    return useMemo(() => {
        if (referencedElement?.type !== ElementType.Select) {
            return [];
        }

        const options = isCodeListSelect
            ? codeListOptions.get(referencedElement.id) ?? []
            : (referencedElement.options ?? []).map((option) => typeof option === 'string'
                ? {value: option, label: option}
                : {value: option.value, label: option.label});

        // A dependent code list select offers a value once per group, but a group value is needed only once.
        const optionsByValue = new Map<string, {value: string; label: string}>();
        for (const option of options) {
            if (option.value.length > 0 && !optionsByValue.has(option.value)) {
                optionsByValue.set(option.value, {value: option.value, label: option.label});
            }
        }
        return Array.from(optionsByValue.values());
    }, [codeListOptions, isCodeListSelect, referencedElement]);
}

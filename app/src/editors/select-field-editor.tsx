import React, {useMemo} from 'react';
import {type BaseEditorProps} from './base-editor';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementType} from '../data/element-type/element-type';
import {flattenElements} from '../utils/flatten-elements';
import {isLoadedForm} from '../slices/app-slice';
import {generateComponentTitle} from '../utils/generate-component-title';

export function SelectFieldEditor(props: BaseEditorProps<SelectFieldElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
        scope,
        entity,
    } = props;

    const rootElement = useMemo(() => {
        return isLoadedForm(entity) ? entity.version.rootElement : entity.rootElement;
    }, [entity]);

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

    const dependencyOptions = useMemo(() => {
        const allElements = flattenElements(rootElement);
        const currentElementIndex = allElements.findIndex((candidate) => candidate.id === element.id);
        const relevantElements = currentElementIndex >= 0 ? allElements.slice(0, currentElementIndex) : allElements;

        return relevantElements
            .filter((candidate) => candidate.type === ElementType.Select && candidate.id !== element.id)
            .map((candidate) => ({
                label: `${generateComponentTitle(candidate)} (${candidate.id})`,
                value: candidate.id,
            }));
    }, [element.id, rootElement]);

    return (
        <>
            <SelectFieldComponent
                label="Abhängiges Auswahlfeld"
                hint="Optional. Wenn gesetzt, werden nur Optionen angezeigt, deren Gruppenwert dem Wert des ausgewählten übergeordneten Auswahlfelds entspricht. Leere Gruppen bleiben immer sichtbar."
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

            <OptionListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die antragstellende Person kann genau eine dieser Optionen auswählen. Der optionale Gruppenwert dient zur Vorfilterung über ein übergeordnetes Auswahlfeld."
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
                showGroupField
                groupLabel="Gruppenwert"
            />

            {
                scope !== 'data_modelling' &&
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
            }
        </>
    );
}

import React, {useMemo} from 'react';
import Grid from '@mui/material/Grid';
import {type BaseEditorProps} from './base-editor';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementType} from '../data/element-type/element-type';
import {useElementTreeContext} from '../components/element-tree-2/element-tree-context';
import {generateComponentPath, generateComponentTitle} from '../utils/generate-component-title';
import {type ProcessDataKeyInputFieldElement} from '../models/elements/form/input/process-data-key-input-field-element';

export function ProcessDataKeyInputFieldEditor(props: BaseEditorProps<ProcessDataKeyInputFieldElement>) {
    const {
        element,
        editable,
        onPatch,
    } = props;

    const {
        allElements,
    } = useElementTreeContext();

    const scopeOptions = useMemo(() => {
        const currentElementIndex = allElements.findIndex(({element: candidate}) => candidate.id === element.id);
        const relevantElements = currentElementIndex >= 0 ? allElements.slice(0, currentElementIndex) : allElements;

        return relevantElements
            .filter(({element: candidate}) => candidate.type === ElementType.ProcessDataKeyInput && candidate.id !== element.id)
            .map(({
                      element: candidate,
                      parents,
                  }) => ({
                label: generateComponentTitle(candidate),
                subLabel: generateComponentPath(parents),
                value: candidate.id,
            }));
    }, [allElements, element.id]);

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <CheckboxFieldComponent
                    label="Wildcards deaktivieren"
                    value={element.disableWildCards ?? false}
                    onChange={(disableWildCards) => {
                        onPatch({
                            disableWildCards,
                        });
                    }}
                    hint="Verhindert Prozessdaten-Schlüssel mit Sternchen."
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <SelectFieldComponent
                    label="Untergeordnete Schlüssel von"
                    value={element.scopeProcessDataKeyInputElementId ?? undefined}
                    onChange={(scopeProcessDataKeyInputElementId) => {
                        onPatch({
                            scopeProcessDataKeyInputElementId,
                        });
                    }}
                    options={scopeOptions}
                    disabled={!editable}
                    emptyStatePlaceholder="Keine vorherigen Prozessdaten-Schlüssel-Felder verfügbar"
                    hint="Beschränkt die Auswahl auf untergeordnete Schlüssel des hier gewählten Prozessdaten-Schlüssels."
                />
            </Grid>
        </Grid>
    );
}

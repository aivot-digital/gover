import React from 'react';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {CodeListSelectField} from '../modules/code-lists/components/code-list-select-field';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {BaseEditorProps} from './base-editor';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {Grid} from '@mui/material';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';
import {ElementEditorSectionHeader} from '../components/element-editor-section-header/element-editor-section-header';

export function TextFieldEditor(props: BaseEditorProps<TextFieldElement>) {
    const {
        element,
        onPatch,
        editable,
        scope,
        hasSummaryLayoutParent,
    } = props;

    if (hasSummaryLayoutParent) {
        return null;
    }

    // Empty rows saved by the former table editor are not actual suggestions.
    const suggestions = (element.suggestions ?? []).filter((suggestion) => suggestion.trim().length > 0);

    return (
        <>
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
                    <TextFieldComponent
                        value={element.placeholder}
                        label="Platzhalter"
                        onChange={(value) => {
                            onPatch({
                                placeholder: value,
                            });
                        }}
                        hint={'Ein Platzhalter zeigt ein Beispiel für die erwartete Eingabe an, z. B. „hallo@bad-musterstadt.de“ bei einer E-Mail-Adresse.'}
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
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
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <NumberFieldComponent
                        label="Minimalanzahl an Zeichen"
                        value={element.minCharacters ?? undefined}
                        onChange={(val) => {
                            onPatch({
                                minCharacters: val,
                            });
                        }}
                        error={element.maxCharacters != null && element.minCharacters != null && element.minCharacters > element.maxCharacters ? 'Sie fordern mehr Zeichen als Sie maximal zulassen.' : undefined}
                        hint="Geben Sie 0 oder nichts ein, um keine Minimalanzahl zu fordern."
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <NumberFieldComponent
                        label="Maximalanzahl an Zeichen"
                        value={element.maxCharacters ?? undefined}
                        onChange={(val) => {
                            onPatch({
                                maxCharacters: val,
                            });
                        }}
                        error={element.maxCharacters != null && element.minCharacters != null && element.maxCharacters < element.minCharacters ? 'Sie fordern mehr Zeichen als Sie maximal zulassen.' : undefined}
                        hint="Geben Sie 0 oder nichts ein, um keine Maximalanzahl zu fordern."
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                />
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <CheckboxFieldComponent
                        label="Mehrzeilige Texteingabe"
                        value={element.isMultiline ?? undefined}
                        onChange={(checked) => {
                            onPatch({
                                isMultiline: checked,
                            });
                        }}
                        disabled={!editable}
                        hint={'Ermöglicht die Eingabe mehrzeiliger Texte statt einer einzelnen Zeile.'}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <CheckboxFieldComponent
                        label="Kopieren des Wertes in Zwischenablage ermöglichen"
                        value={element.copyable ?? false}
                        onChange={(checked) => {
                            onPatch({
                                copyable: checked,
                            });
                        }}
                        disabled={!editable}
                        hint={'Zeigt am Ende des Eingabefelds eine Schaltfläche zum einfachen Kopieren des aktuellen Wertes in die Zwischenablage ein.'}
                    />
                </Grid>
                {!element.isMultiline && element.dynamicTextPolicy == null && <>
                    <Grid size={{xs: 12}}>
                        <ElementEditorSectionHeader title="Vorschläge zur Texteingabe" variant="h6">
                            Hinterlegen Sie häufige Eingaben manuell oder wählen Sie eine System-Codeliste.
                            Die Vorschläge werden beim Tippen gefiltert und können übernommen werden.
                            Eigene Texte bleiben möglich.
                        </ElementEditorSectionHeader>
                    </Grid>
                    <Grid size={{xs: 12, lg: 6}}>
                        <SelectFieldComponent
                            label="Vorschläge definieren über"
                            value={element.optionsSource ?? OptionsSourceType.Manual}
                            options={[
                                {value: OptionsSourceType.Manual, label: 'Manuelle Eingabe'},
                                {value: OptionsSourceType.CodeList, label: 'System-Codeliste'},
                            ]}
                            onChange={(value) => onPatch({optionsSource: value ?? OptionsSourceType.Manual})}
                            disabled={!editable}
                            required
                        />
                    </Grid>
                    {element.optionsSource === OptionsSourceType.CodeList ? <Grid size={{xs: 12, lg: 6}}>
                        <CodeListSelectField
                            value={element.codeListKey}
                            onChange={(codeListKey) => onPatch({codeListKey})}
                            hint="Die Beschriftungen werden als Text vorgeschlagen. Eigene Eingaben bleiben möglich."
                            disabled={!editable}
                            required
                        />
                    </Grid> : <Grid size={{xs: 12, lg: 6}}>
                        <ChipInputFieldComponent
                            label="Vorschläge"
                            value={suggestions}
                            onChange={(suggestions) => onPatch({suggestions: suggestions ?? []})}
                            disabled={!editable}
                            hint="Geben Sie Vorschläge einzeln ein oder fügen Sie mehrere Zeilen aus der Zwischenablage ein."
                        />
                    </Grid>}
                </>}
            </Grid>
        </>
    );
};

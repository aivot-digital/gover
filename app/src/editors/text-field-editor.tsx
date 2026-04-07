import React from 'react';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {BaseEditorProps} from './base-editor';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';

export function TextFieldEditor(props: BaseEditorProps<TextFieldElement, ElementTreeEntity>) {
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
            </Grid>
        </>
    );
};

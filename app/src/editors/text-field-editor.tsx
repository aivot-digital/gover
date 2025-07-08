import React from 'react';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {type BaseEditor} from './base-editor';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {HtmlAutofillAttributeOptions} from "../data/html-autofill-attribute-options";
import {Autocomplete, Box, Grid, TextField, Typography} from "@mui/material";
import {getAutofillOptionsForElementType} from "../data/element-type/element-autofill-options";
import {ElementType} from "../data/element-type/element-type";

export const TextFieldEditor: BaseEditor<TextFieldElement, ElementTreeEntity> = ({
                                                                                     element,
                                                                                     onPatch,
                                                                                     editable,
                                                                                 }) => {
    return (
        <>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <TextFieldComponent
                        value={element.placeholder}
                        label="Platzhalter"
                        onChange={(value) => {
                            onPatch({
                                placeholder: value,
                            });
                        }}
                        hint={"Ein Platzhalter zeigt ein Beispiel für die erwartete Eingabe an, z. B. „hallo@bad-musterstadt.de“ bei einer E-Mail-Adresse."}
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>

                    <Autocomplete
                        id="autocomplete-select"
                        value={HtmlAutofillAttributeOptions.find(item => item.value === element.autocomplete) ?? null}
                        onChange={(event, val) => {
                            onPatch({
                                autocomplete: val?.value,
                            });
                        }}
                        options={getAutofillOptionsForElementType(ElementType.Text)}
                        autoHighlight
                        getOptionLabel={(option) => option.label + ' (' + option.value + ')'}
                        renderOption={(props, option) => (
                            <Box component="li" sx={{display: 'block!important'}} {...props}>
                                <Typography
                                    component={'div'}
                                    variant="body1"
                                >
                                    <b>{option.label}</b> ({option.value})
                                </Typography>
                                <Typography
                                    component={'div'}
                                    variant="caption"
                                    color="textSecondary"
                                    sx={{maxWidth: 740, my: 0}}
                                >
                                    {option.description}
                                </Typography>
                            </Box>
                        )}
                        renderInput={(params) => (
                            <TextField
                                {...params}
                                label="Automatisches Ausfüllen durch den Browser (Autocomplete)"
                                inputProps={{
                                    ...params.inputProps,
                                    autoComplete: 'new-password', // disable autocomplete and autofill
                                }}
                                helperText={"Legen Sie fest, welches Datenfeld der Browser zur Autovervollständigung vorschlagen soll (z. B. Name, E-Mail). Vorschläge sind browserabhängig."}
                            />
                        )}
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4
                    }}>
                    <NumberFieldComponent
                        label="Minimalanzahl an Zeichen"
                        value={element.minCharacters}
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
                        lg: 4
                    }}>
                    <NumberFieldComponent
                        label="Maximalanzahl an Zeichen"
                        value={element.maxCharacters}
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
                        lg: 4
                    }}>
                    <CheckboxFieldComponent
                        label="Mehrzeilige Texteingabe"
                        value={element.isMultiline}
                        onChange={(checked) => {
                            onPatch({
                                isMultiline: checked,
                            });
                        }}
                        disabled={!editable}
                        hint={"Ermöglicht die Eingabe mehrzeiliger Texte statt einer einzelnen Zeile."}
                    />
                </Grid>
            </Grid>
        </>
    );
};

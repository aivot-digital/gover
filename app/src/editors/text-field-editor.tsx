import React from 'react';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {type BaseEditor} from './base-editor';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {HtmlAutofillAttributeOptions} from "../data/html-autofill-attribute-options";
import {Autocomplete, Box, TextField, Typography} from "@mui/material";
import {getAutofillOptionsForElementType} from "../data/element-type/element-autofill-options";
import {ElementType} from "../data/element-type/element-type";

export const TextFieldEditor: BaseEditor<TextFieldElement, ElementTreeEntity> = ({
                                                                                     element,
                                                                                     onPatch,
                                                                                     editable,
                                                                                 }) => {
    return (
        <>
            <TextFieldComponent
                value={element.placeholder}
                label="Platzhalter"
                onChange={(value) => {
                    onPatch({
                        placeholder: value,
                    });
                }}
                hint={"Ein Platzhalter ist eine Musterangabe, die zeigt, welche Information eingegeben werden sollen und erwartet werden. Bei einem E-Mail-Feld wäre eine Möglichkeit z.B. „hallo@bad-musterstadt.de“."}
                disabled={!editable}
            />

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
                        helperText={"Definieren Sie ein Datenattribut, das der Browser für dieses Feld vorschlagen soll. Benutzer können diese Vorschläge (abhängig vom verwendeten Browser) auswählen, um das Element automatisch auszufüllen."}
                    />
                )}
            />

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

            <CheckboxFieldComponent
                label="Mehrzeilige Texteingabe"
                value={element.isMultiline}
                onChange={(checked) => {
                    onPatch({
                        isMultiline: checked,
                    });
                }}
                disabled={!editable}
            />
        </>
    );
};

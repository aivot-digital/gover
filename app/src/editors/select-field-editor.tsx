import React from 'react';
import {type BaseEditorProps} from './base-editor';
import {
    type SelectFieldElement,
    type SelectFieldElementOption
} from '../models/elements/form/input/select-field-element';
import {StringListInput} from '../components/string-list-input/string-list-input';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {isStringArray} from '../utils/is-string-array';
import {HtmlAutofillAttributeOptions} from "../data/html-autofill-attribute-options";
import {getAutofillOptionsForElementType} from "../data/element-type/element-autofill-options";
import {ElementType} from "../data/element-type/element-type";
import {Autocomplete, Box, TextField, Typography} from "@mui/material";

export function SelectFieldEditor(props: BaseEditorProps<SelectFieldElement, ElementTreeEntity>): JSX.Element {
    const options = props.element.options ?? [];


    return (
        <>
            {
                (isStringArray(options) && options.length > 0) ?
                    <StringListInput
                        label="Optionen"
                        addLabel="Option hinzufügen"
                        hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
                        noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                        value={options}
                        onChange={(options) => {
                            props.onPatch({
                                options,
                            });
                        }}
                        allowEmpty={false}
                        disabled={!props.editable}
                    />
                    :
                    <OptionListInput
                        label="Optionen"
                        addLabel="Option hinzufügen"
                        hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
                        noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                        value={options as SelectFieldElementOption[]}
                        onChange={(options) => {
                            props.onPatch({
                                options,
                            });
                        }}
                        allowEmpty={false}
                        disabled={!props.editable}
                    />
            }

            <Autocomplete
                id="autocomplete-select"
                value={HtmlAutofillAttributeOptions.find(item => item.value === props.element.autocomplete) ?? null}
                onChange={(event, val) => {
                    props.onPatch({
                        autocomplete: val?.value,
                    });
                }}
                options={getAutofillOptionsForElementType(ElementType.Select)}
                autoHighlight
                getOptionLabel={(option) => option.label + ' (' + option.value + ')'}
                renderOption={(props, option) => (
                    <Box component="li"
                         sx={{display: 'block!important'}} {...props}>
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

        </>
    );
}

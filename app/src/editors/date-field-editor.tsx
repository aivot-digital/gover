import {type BaseEditor} from './base-editor';
import {DateFieldComponentModelMode, type DateFieldElement} from '../models/elements/form/input/date-field-element';
import {type SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {HtmlAutofillAttributeOptions} from "../data/html-autofill-attribute-options";
import {getAutofillOptionsForElementType} from "../data/element-type/element-autofill-options";
import {ElementType} from "../data/element-type/element-type";
import {Autocomplete, Box, TextField, Typography} from "@mui/material";

const modes: SelectFieldComponentOption[] = [
    {
        value: DateFieldComponentModelMode.Day,
        label: 'TT.MM.JJJJ',
    },
    {
        value: DateFieldComponentModelMode.Month,
        label: 'MM.JJJJ',
    },
    {
        value: DateFieldComponentModelMode.Year,
        label: 'JJJJ',
    },
];

export const DateFieldEditor: BaseEditor<DateFieldElement, ElementTreeEntity> = ({
                                                                                     element,
                                                                                     onPatch,
                                                                                     editable,
                                                                                 }) => {
    return (
        <>
            <SelectFieldComponent
                label="Datums-Format"
                value={element.mode ?? DateFieldComponentModelMode.Day}
                onChange={(val) => {
                    onPatch({
                        mode: val as DateFieldComponentModelMode,
                    });
                }}
                options={modes}
                required
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
                options={getAutofillOptionsForElementType(ElementType.Date)}
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
        </>
    );
};

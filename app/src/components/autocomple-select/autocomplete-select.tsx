import {HtmlAutofillAttributeOptions} from '../../data/html-autofill-attribute-options';
import {getAutofillOptionsForElementType} from '../../data/element-type/element-autofill-options';
import {Autocomplete, Box, TextField, Typography} from '@mui/material/esm';
import React, {useMemo} from 'react';
import {ElementType} from '../../data/element-type/element-type';

interface AutocompleteSelectProps {
    type: ElementType;
    value: string | null | undefined;
    onChange: (value: string | undefined) => void;
    editable: boolean;
}

export function AutocompleteSelect(props: AutocompleteSelectProps) {
    const {
        type,
        value,
        onChange,
        editable,
    } = props;

    const autofillOptions = useMemo(() => {
        return getAutofillOptionsForElementType(type);
    }, [type]);

    const selectedAttribute = useMemo(() => {
        return HtmlAutofillAttributeOptions.find(item => item.value === value) ?? null;
    }, [value]);

    return (
        <Autocomplete
            id="autocomplete-select"
            value={selectedAttribute}
            onChange={(_, val) => {
                onChange(val?.value ?? undefined);
            }}
            options={autofillOptions}
            autoHighlight
            getOptionLabel={(option) => option.label + ' (' + option.value + ')'}
            renderOption={(props, option) => (
                <Box
                    component="li"
                    sx={{display: 'block!important'}} {...props}>
                    <Typography
                        component={'div'}
                        variant="body1"
                    >
                        <b>{option.label}</b>
                        ({option.value})
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
                    helperText={'Legen Sie fest, welches Datenfeld der Browser zur Autovervollständigung vorschlagen soll (z. B. Name, E-Mail). Vorschläge sind browserabhängig.'}
                />
            )}
            disabled={!editable}
        />
    );
}
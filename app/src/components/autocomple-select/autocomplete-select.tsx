import {HtmlAutofillAttributeOptions} from '../../data/html-autofill-attribute-options';
import {getAutofillOptionsForElementType} from '../../data/element-type/element-autofill-options';
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import React, {useMemo} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {FormField, type FormFieldLayoutProps, getNativeInputAriaProps} from '../form-field';
import {formFieldInputRootSx} from '../../theming/form-field-tokens';

interface AutocompleteSelectProps extends FormFieldLayoutProps {
    type: ElementType;
    value: string | null | undefined;
    onChange: (value: string | undefined) => void;
    editable: boolean;
    label?: string;
    hint?: string;
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
    const label = props.label ?? 'Automatisches Ausfüllen durch den Browser (Autocomplete)';
    const hint = props.hint ?? 'Legen Sie fest, welches Datenfeld der Browser zur Autovervollständigung vorschlagen soll (z. B. Name, E-Mail). Vorschläge sind browserabhängig.';

    return (
        <FormField
            id={props.id}
            label={label}
            hint={hint}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            disabled={!editable}
            margin={props.margin}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(field) => (
                <Autocomplete
                    id={field.controlId}
                    value={selectedAttribute}
                    onChange={(_, val) => {
                        onChange(val?.value ?? undefined);
                    }}
                    options={autofillOptions}
                    autoHighlight
                    sx={{
                        '& .MuiInputBase-root': formFieldInputRootSx,
                    }}
                    getOptionLabel={(option) => option.label + ' (' + option.value + ')'}
                    renderOption={({key, ...optionProps}, option) => (
                        <Box
                            key={key}
                            component="li"
                            sx={{display: 'block!important'}}
                            {...optionProps}
                        >
                            <Typography component="div" variant="body1">
                                <b>{option.label}</b>{' '}({option.value})
                            </Typography>
                            <Typography
                                component="div"
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
                            size="small"
                            margin="none"
                            slotProps={{
                                ...params.slotProps,
                                htmlInput: {
                                    ...params.slotProps.htmlInput,
                                    ...getNativeInputAriaProps(field, params.slotProps.htmlInput),
                                    autoComplete: 'new-password',
                                },
                            }}
                        />
                    )}
                    disabled={!editable}
                />
            )}
        </FormField>
    );
}

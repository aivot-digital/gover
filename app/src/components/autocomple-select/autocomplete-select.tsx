import {HtmlAutofillAttributeOptions, type HtmlAutofillAttributeOption} from '../../data/html-autofill-attribute-options';
import {getAutofillOptionsForElementType} from '../../data/element-type/element-autofill-options';
import Autocomplete, {createFilterOptions} from '@mui/material/Autocomplete';
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

const filterAutofillOptions = createFilterOptions<HtmlAutofillAttributeOption>({
    trim: true,
    stringify: (option) => [option.label, option.value, option.description, option.group].join(' '),
});

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
    const showGroups = useMemo(() => (
        new Set(autofillOptions.map(option => option.group)).size > 1
    ), [autofillOptions]);

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
            externalAction={props.externalAction}
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
                    filterOptions={filterAutofillOptions}
                    isOptionEqualToValue={(option, selectedOption) => option.value === selectedOption.value}
                    groupBy={showGroups ? (option) => option.group : undefined}
                    renderGroup={(params) => (
                        <li key={params.key} role="group" aria-label={params.group}>
                            <Box sx={{px: 2, py: 0.5, bgcolor: 'action.hover'}}>
                                <Typography variant="caption" sx={{fontWeight: 600, color: 'text.secondary'}}>
                                    {params.group}
                                </Typography>
                            </Box>
                            <Box component="ul" role="presentation" sx={{p: 0, m: 0}}>
                                {params.children}
                            </Box>
                        </li>
                    )}
                    autoHighlight
                    sx={{
                        '& .MuiInputBase-root': formFieldInputRootSx,
                    }}
                    getOptionLabel={(option) => option.label + ' (' + option.value + ')'}
                    renderOption={({key, ...optionProps}, option) => (
                        <Box
                            key={key}
                            component="li"
                            {...optionProps}
                            sx={{display: 'block!important'}}
                        >
                            <Box sx={{display: 'flex', alignItems: 'baseline', flexWrap: 'wrap', columnGap: 1}}>
                                <Typography component="span" variant="body2">
                                    {option.label}
                                </Typography>
                                {' '}
                                <Typography component="code" variant="caption" color="text.secondary"
                                            sx={{fontFamily: 'monospace'}}>
                                    {option.value}
                                </Typography>
                            </Box>
                            <Typography
                                component="div"
                                variant="caption"
                                color="text.secondary"
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

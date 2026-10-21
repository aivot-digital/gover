import {useMemo} from 'react';
import {Autocomplete, Checkbox, TextField} from '@mui/material';
import {FormField, getNativeInputAriaProps} from '../form-field';
import {formFieldInputRootSx} from '../../theming/form-field-tokens';
import {type MultiCheckboxComponentProps, type MultiCheckboxOptions} from './multi-checkbox-component';
import {createOptionFilter} from '../select-field/option-search';

const filterOptions = createOptionFilter<MultiCheckboxOptions>((option) => [option.label, option.value]);

export function MultiChoiceCombobox(props: MultiCheckboxComponentProps) {
    const {labelAction} = props;
    const locked = Boolean(props.disabled || props.busy);
    const selected = useMemo(() => (props.value ?? []).map((value) => props.options.find((option) => option.value === value)
        ?? {value, label: value}), [props.value, props.options]);

    return <FormField
        {...props}
        labelAction={typeof labelAction === 'function'
            ? (field) => labelAction({
                ...field, labelId: field.labelId ?? '', groupId: field.controlId, describedBy: field.ariaProps['aria-describedby'],
            })
            : labelAction}
        margin={props.margin ?? 'normal'}
    >
        {(field) => <Autocomplete
            id={field.controlId}
            multiple
            disableCloseOnSelect
            fullWidth
            sx={props.controlSx}
            size="small"
            disabled={props.disabled}
            readOnly={props.busy}
            options={props.options}
            value={selected}
            getOptionLabel={(option) => option.label}
            getOptionKey={(option) => option.value}
            isOptionEqualToValue={(option, value) => option.value === value.value}
            filterOptions={filterOptions}
            noOptionsText={props.options.length === 0 ? 'Keine Optionen vorhanden' : 'Keine passenden Optionen gefunden'}
            onChange={(_, options) => {
                if (locked) return;
                const values = options.map((option) => option.value);
                props.onChange(values.length > 0 ? values : null);
            }}
            renderOption={({key, ...optionProps}, option, {selected}) => <li key={key} {...optionProps}>
                <Checkbox checked={selected} tabIndex={-1} disableRipple
                    slotProps={{input: {'aria-hidden': true}}} sx={{pointerEvents: 'none'}}/>
                {option.label}
            </li>}
            renderInput={(params) => <TextField
                {...params}
                margin="none"
                placeholder={selected.length === 0 ? 'Optionen auswählen' : undefined}
                error={field.invalid}
                slotProps={{
                    ...params.slotProps,
                    input: {...params.slotProps.input, sx: formFieldInputRootSx},
                    htmlInput: {...params.slotProps.htmlInput,
                        ...getNativeInputAriaProps(field, params.slotProps.htmlInput)},
                }}
            />}
        />}
    </FormField>;
}

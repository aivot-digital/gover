import React, {useMemo} from 'react';
import {
    Autocomplete,
    type AutocompleteRenderInputParams,
    Box,
    createFilterOptions,
    InputAdornment,
    ListItemIcon,
    ListItemText,
    MenuItem,
    TextField,
} from '@mui/material';
import Check from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import {type SelectFieldComponentProps} from './select-field-component-props';
import {
    type SelectFieldComponentOption,
    type SelectFieldValue,
} from './select-field-component-option';
import {renderIconButton} from '../text-field/text-field-component';
import {
    FormField,
    type FormFieldControlContext,
    getCompositeControlAriaProps,
    getNativeInputAriaProps,
    mergeAriaIds,
} from '../form-field';
import type {TextFieldOwnerState} from '@mui/material/TextField';
import {formFieldInputRootSx} from '../../theming/form-field-tokens';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';

/**
 * Select values are string-backed in authored element data, while application forms also use numeric IDs.
 * Normalizing only for control lookup preserves both contracts: matching is tolerant, emitted option values stay typed.
 */
function normalizeValue(value: SelectFieldValue | null | undefined): string {
    return value == null ? '' : String(value);
}

function mergeAdornments(...adornments: React.ReactNode[]): React.ReactNode {
    const presentAdornments = adornments.filter((adornment) => adornment != null);
    if (presentAdornments.length === 0) {
        return undefined;
    }

    if (presentAdornments.length === 1) {
        return presentAdornments[0];
    }

    return <>{presentAdornments}</>;
}

function findOption<T extends SelectFieldValue>(
    options: SelectFieldComponentOption<T>[],
    value: SelectFieldValue | null | undefined,
): SelectFieldComponentOption<T> | undefined {
    const normalizedValue = normalizeValue(value);
    if (normalizedValue.length === 0) {
        return undefined;
    }

    return options.find((option) => normalizeValue(option.value) === normalizedValue);
}

function renderStartAdornment(startIcon: React.ReactNode): React.ReactNode {
    return startIcon != null
        ? <InputAdornment position="start">{startIcon}</InputAdornment>
        : undefined;
}

function renderEndAdornment(endAction: SelectFieldComponentProps['endAction']): React.ReactNode {
    if (endAction == null) {
        return undefined;
    }

    return (
        <InputAdornment position="end" sx={{mr: 2}}>
            {Array.isArray(endAction)
                ? endAction.map(renderIconButton)
                : renderIconButton(endAction)}
        </InputAdornment>
    );
}

function resolveSlotProps<T>(
    slotProps: T | ((ownerState: TextFieldOwnerState) => T) | undefined,
    ownerState: TextFieldOwnerState,
): T | undefined {
    return typeof slotProps === 'function'
        ? (slotProps as (ownerState: TextFieldOwnerState) => T)(ownerState)
        : slotProps;
}

interface SelectControlProps<T extends SelectFieldValue> {
    props: SelectFieldComponentProps<T>;
    fieldContext: FormFieldControlContext;
}

function DropdownControl<T extends SelectFieldValue>({props, fieldContext}: SelectControlProps<T>) {
    const {
        autocomplete,
        placeholder,
        disabled,
        readOnly,
        busy,
        required,
        value,
        onChange,
        options,
        emptyStatePlaceholder,
        includeEmptyOption = true,
        startIcon,
        endAction,
        controlSx,
        muiPassTroughProps,
        size = 'small',
    } = props;
    const passThroughSlotProps = muiPassTroughProps?.slotProps;
    const selectedOption = findOption(options, value);

    return (
        <TextField
            {...muiPassTroughProps}
            id={fieldContext.controlId}
            select
            label={undefined}
            autoComplete={autocomplete}
            required={required}
            error={fieldContext.invalid}
            helperText={undefined}
            placeholder={placeholder}
            value={selectedOption != null ? normalizeValue(selectedOption.value) : ''}
            onChange={(event) => {
                const option = findOption(options, event.target.value);
                onChange(option?.value ?? null);
            }}
            disabled={disabled ?? false}
            size={size}
            fullWidth
            margin="none"
            slotProps={{
                ...passThroughSlotProps,
                input: (ownerState: TextFieldOwnerState) => {
                    const inputSlotProps = resolveSlotProps(passThroughSlotProps?.input, ownerState);

                    return {
                        ...inputSlotProps,
                        sx: [inputSlotProps?.sx, formFieldInputRootSx, controlSx],
                        readOnly: readOnly || busy || inputSlotProps?.readOnly,
                        startAdornment: mergeAdornments(
                            renderStartAdornment(startIcon),
                            inputSlotProps?.startAdornment,
                        ),
                        endAdornment: mergeAdornments(
                            renderEndAdornment(endAction),
                            inputSlotProps?.endAdornment,
                        ),
                    };
                },
                select: (ownerState: TextFieldOwnerState) => {
                    const selectSlotProps = resolveSlotProps(passThroughSlotProps?.select, ownerState);
                    const ariaProps = getCompositeControlAriaProps(fieldContext, selectSlotProps);
                    const labelId = mergeAriaIds(
                        fieldContext.labelId,
                        selectSlotProps?.labelId,
                        ariaProps['aria-labelledby'],
                    );

                    return {
                        ...selectSlotProps,
                        ...ariaProps,
                        // MUI otherwise applies aria-labelledby to both the InputBase wrapper and the
                        // actual combobox. labelId keeps the accessible name on the interactive element.
                        'aria-label': labelId == null ? ariaProps['aria-label'] : undefined,
                        'aria-labelledby': undefined,
                        labelId,
                        readOnly: readOnly || busy || selectSlotProps?.readOnly,
                        renderValue: (selectedValue: unknown) => {
                            return findOption(options, normalizeValue(selectedValue as SelectFieldValue))?.label ?? '';
                        },
                        MenuProps: {
                            ...selectSlotProps?.MenuProps,
                            slotProps: {
                                ...selectSlotProps?.MenuProps?.slotProps,
                                paper: {
                                    sx: {
                                        width: 0,
                                    },
                                },
                            },
                        },
                    };
                },
            }}
        >
            {includeEmptyOption && !(required ?? false) && options.length > 0 && (
                <MenuItem value="">
                    <i>{placeholder ?? 'Keine Auswahl'}</i>
                </MenuItem>
            )}

            {options.length === 0 && (
                <MenuItem value="">
                    <i>{emptyStatePlaceholder ?? 'Keine Optionen vorhanden'}</i>
                </MenuItem>
            )}

            {options.map((option) => (
                <MenuItem
                    key={normalizeValue(option.value)}
                    value={normalizeValue(option.value)}
                    sx={{
                        maxWidth: '100%',
                        overflow: 'hidden',
                    }}
                >
                    {option.icon != null && <ListItemIcon>{option.icon}</ListItemIcon>}
                    <ListItemText
                        primary={option.label}
                        secondary={option.subLabel}
                        sx={{minWidth: 0}}
                        slotProps={{
                            primary: {noWrap: true},
                            secondary: {noWrap: true},
                        }}
                    />
                </MenuItem>
            ))}
        </TextField>
    );
}

function ComboboxControl<T extends SelectFieldValue>({props, fieldContext}: SelectControlProps<T>) {
    const {
        autocomplete,
        placeholder,
        disabled,
        readOnly,
        busy,
        required,
        value,
        onChange,
        options,
        emptyStatePlaceholder,
        includeEmptyOption = true,
        startIcon,
        endAction,
        controlSx,
        muiPassTroughProps,
        size = 'small',
    } = props;
    const selectedOption = findOption(options, value) ?? null;
    const passThroughSlotProps = muiPassTroughProps?.slotProps;
    const filterOptions = useMemo(() => createFilterOptions<SelectFieldComponentOption<T>>({
        stringify: (option) => [
            option.label,
            option.subLabel,
            normalizeValue(option.value),
        ].filter(Boolean).join(' '),
    }), []);

    const renderInput = (params: AutocompleteRenderInputParams) => (
        <TextField
            {...muiPassTroughProps}
            {...params}
            id={fieldContext.controlId}
            label={undefined}
            autoComplete={autocomplete}
            required={required}
            error={fieldContext.invalid}
            helperText={undefined}
            placeholder={placeholder}
            disabled={disabled ?? false}
            size={size}
            fullWidth
            margin="none"
            slotProps={{
                ...params.slotProps,
                ...passThroughSlotProps,
                input: (ownerState: TextFieldOwnerState) => {
                    const inputSlotProps = resolveSlotProps(passThroughSlotProps?.input, ownerState);

                    return {
                        ...inputSlotProps,
                        ...params.slotProps.input,
                        sx: [inputSlotProps?.sx, formFieldInputRootSx, controlSx],
                        readOnly: readOnly || busy || inputSlotProps?.readOnly,
                        startAdornment: mergeAdornments(
                            renderStartAdornment(startIcon),
                            inputSlotProps?.startAdornment,
                            params.slotProps.input?.startAdornment,
                        ),
                        endAdornment: mergeAdornments(
                            renderEndAdornment(endAction),
                            inputSlotProps?.endAdornment,
                            params.slotProps.input?.endAdornment,
                        ),
                    };
                },
                htmlInput: (ownerState: TextFieldOwnerState) => {
                    const htmlInputSlotProps = resolveSlotProps(passThroughSlotProps?.htmlInput, ownerState);
                    const combinedHtmlInputProps = {
                        ...htmlInputSlotProps,
                        ...params.slotProps.htmlInput,
                    };

                    return {
                        ...combinedHtmlInputProps,
                        ...getNativeInputAriaProps(fieldContext, combinedHtmlInputProps),
                        autoComplete: autocomplete ?? combinedHtmlInputProps.autoComplete,
                    };
                },
            }}
        />
    );

    return (
        <Autocomplete
            id={fieldContext.controlId}
            options={options}
            value={selectedOption}
            onChange={(_, option) => {
                if (!busy) {
                    onChange(option?.value ?? null);
                }
            }}
            getOptionKey={(option) => normalizeValue(option.value)}
            getOptionLabel={(option) => option.label}
            isOptionEqualToValue={(option, selectedValue) => (
                normalizeValue(option.value) === normalizeValue(selectedValue.value)
            )}
            filterOptions={filterOptions}
            autoHighlight
            disableClearable={Boolean(required) || !includeEmptyOption}
            readOnly={readOnly || busy}
            disabled={disabled}
            noOptionsText={emptyStatePlaceholder ?? 'Keine Optionen vorhanden'}
            fullWidth
            renderOption={({key, ...optionProps}, option, state) => (
                <Box
                    component="li"
                    key={key}
                    {...optionProps}
                    sx={{
                        minHeight: 40,
                        py: 0.5,
                    }}
                >
                    {option.icon != null && <ListItemIcon>{option.icon}</ListItemIcon>}
                    <ListItemText
                        primary={option.label}
                        secondary={option.subLabel}
                        sx={{minWidth: 0}}
                        slotProps={{
                            primary: {noWrap: true},
                            secondary: {noWrap: true},
                        }}
                    />
                    <Check
                        sx={{
                            ml: 1,
                            flexShrink: 0,
                            fontSize: 18,
                            color: 'primary.main',
                            opacity: state.selected ? 1 : 0,
                        }}
                    />
                </Box>
            )}
            renderInput={renderInput}
        />
    );
}

export function SelectFieldComponent<T extends SelectFieldValue = string>(props: SelectFieldComponentProps<T>) {
    const {
        label,
        hint,
        disabled,
        readOnly,
        busy,
        required,
        error,
        presentation = SelectFieldPresentation.Dropdown,
        sx,
        margin,
        muiPassTroughProps,
        id,
        ariaLabel,
        ariaDescribedBy,
        labelAction,
        showOptionalIndicator,
    } = props;
    const fieldMargin = muiPassTroughProps?.margin ?? margin ?? 'normal';

    return (
        <FormField
            id={id ?? muiPassTroughProps?.id}
            label={label}
            ariaLabel={ariaLabel}
            ariaDescribedBy={ariaDescribedBy}
            labelAction={labelAction}
            hint={hint}
            error={error}
            required={required}
            disabled={disabled}
            readOnly={readOnly}
            busy={busy}
            margin={fieldMargin}
            showOptionalIndicator={showOptionalIndicator}
            sx={sx}
        >
            {(fieldContext: FormFieldControlContext) => presentation === SelectFieldPresentation.Combobox
                ? <ComboboxControl props={props} fieldContext={fieldContext}/>
                : <DropdownControl props={props} fieldContext={fieldContext}/>
            }
        </FormField>
    );
}

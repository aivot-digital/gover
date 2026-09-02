import {useMemo, type HTMLAttributes, type Key} from 'react';
import {Autocomplete, type AutocompleteRenderInputParams, Box, ListItemText, type SxProps, TextField, type Theme} from '@mui/material';
import type {
    ProcessNodeDefinitionMetadataForwardedAttachmentSet,
} from '../../modules/process/entities/process-node-definition-metadata';
import {FormField, type FormFieldLayoutProps, getNativeInputAriaProps} from '../form-field';

interface AttachmentSetOption {
    dataKey: string;
    label: string;
    subLabel: string;
}

export interface ProcessInstanceAttachmentSetSelectProps extends FormFieldLayoutProps {
    attachmentSets: ProcessNodeDefinitionMetadataForwardedAttachmentSet[] | null | undefined;
    value: string[] | null | undefined;
    onChange: (value: string[] | null) => void;
    label: string;
    placeholder?: string | null;
    hint?: string | null;
    errors?: string[] | null;
    required?: boolean | null;
    disabled?: boolean | null;
    readOnly?: boolean | null;
    busy?: boolean | null;
    maxItems?: number | null;
    controlSx?: SxProps<Theme>;
}

export function ProcessInstanceAttachmentSetSelect(props: ProcessInstanceAttachmentSetSelectProps) {
    const {
        attachmentSets,
        value,
        onChange,
        label,
        placeholder,
        hint,
        errors,
        required,
        disabled,
        readOnly,
        busy,
        maxItems,
    } = props;

    const selectedDataKeys = useMemo(() => {
        return Array.from(new Set((value ?? [])
            .map((dataKey) => dataKey.trim())
            .filter((dataKey) => dataKey.length > 0)));
    }, [value]);

    const {
        options,
        optionsByDataKey,
    } = useMemo(() => {
        const result = new Map<string, AttachmentSetOption>();

        for (const attachmentSet of attachmentSets ?? []) {
            if (result.has(attachmentSet.dataKey)) {
                continue;
            }

            result.set(attachmentSet.dataKey, createAttachmentSetOption(attachmentSet));
        }

        for (const dataKey of selectedDataKeys) {
            if (!result.has(dataKey)) {
                result.set(dataKey, {
                    dataKey,
                    label: dataKey,
                    subLabel: dataKey,
                });
            }
        }

        return {
            options: Array.from(result.values()),
            optionsByDataKey: result,
        };
    }, [attachmentSets, selectedDataKeys]);

    const selectedOptions = useMemo(() => {
        return selectedDataKeys
            .map((dataKey) => optionsByDataKey.get(dataKey))
            .filter((option): option is AttachmentSetOption => option != null);
    }, [optionsByDataKey, selectedDataKeys]);

    const effectiveMaxItems = maxItems != null && maxItems > 0
        ? maxItems
        : undefined;

    const isSingleSelect = effectiveMaxItems === 1;

    const updateSelectedOptions = (updatedOptions: AttachmentSetOption[]) => {
        if (readOnly) {
            return;
        }

        const updatedDataKeys: string[] = [];
        const knownDataKeys = new Set<string>();

        for (const option of updatedOptions) {
            if (knownDataKeys.has(option.dataKey)) {
                continue;
            }

            if (effectiveMaxItems != null && updatedDataKeys.length >= effectiveMaxItems) {
                continue;
            }

            updatedDataKeys.push(option.dataKey);
            knownDataKeys.add(option.dataKey);
        }

        onChange(updatedDataKeys.length > 0 ? updatedDataKeys : null);
    };

    const renderHelperText = (text: string | null | undefined, isError = false) => {
        if ((text == null || text.length === 0) && effectiveMaxItems == null) {
            return undefined;
        }

        return (
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: 2,
                    width: '100%',
                }}
            >
                <Box
                    component="span"
                    sx={{
                        color: isError ? 'error.main' : 'text.secondary',
                    }}
                >
                    {text}
                </Box>

                {
                    effectiveMaxItems != null &&
                    <Box
                        component="span"
                        sx={{
                            color: 'text.secondary',
                            fontVariantNumeric: 'tabular-nums',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {selectedOptions.length}/{effectiveMaxItems}
                    </Box>
                }
            </Box>
        );
    };

    const renderOption = (
        {key, ...optionProps}: HTMLAttributes<HTMLLIElement> & {key: Key},
        option: AttachmentSetOption,
    ) => (
        <Box component="li" key={key} {...optionProps}>
            <ListItemText
                primary={option.label}
                secondary={option.subLabel}
            />
        </Box>
    );

    const renderInput = (params: AutocompleteRenderInputParams, field: Parameters<typeof getNativeInputAriaProps>[0]) => (
        <TextField
            {...params}
            size="small"
            placeholder={selectedOptions.length > 0 ? undefined : placeholder ?? undefined}
            error={field.invalid}
            slotProps={{
                ...params.slotProps,
                htmlInput: {
                    ...params.slotProps.htmlInput,
                    ...getNativeInputAriaProps(field, params.slotProps.htmlInput),
                    readOnly: field.readOnly || field.busy || undefined,
                },
            }}
        />
    );

    return (
        <FormField
            id={props.id}
            label={label}
            hint={renderHelperText(hint)}
            error={errors != null ? renderHelperText(errors.join(' '), true) : undefined}
            required={Boolean(required)}
            disabled={Boolean(disabled)}
            readOnly={Boolean(readOnly)}
            busy={Boolean(busy)}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            margin={props.margin}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(field) => isSingleSelect ? (
                <Autocomplete<AttachmentSetOption, false, false, false>
                    id={field.controlId}
                    fullWidth
                    options={options}
                    value={selectedOptions[0] ?? null}
                    getOptionLabel={(option) => option.label}
                    isOptionEqualToValue={(option, selectedOption) => option.dataKey === selectedOption.dataKey}
                    disabled={field.disabled || field.busy}
                    readOnly={field.readOnly || field.busy}
                    noOptionsText="Keine Anlagensätze verfügbar"
                    onChange={(_, updatedOption) => updateSelectedOptions(updatedOption == null ? [] : [updatedOption])}
                    renderOption={renderOption}
                    renderInput={(params) => renderInput(params, field)}
                    sx={props.controlSx}
                />
            ) : (
                <Autocomplete<AttachmentSetOption, true, false, false>
                    id={field.controlId}
                    multiple
                    fullWidth
                    filterSelectedOptions
                    options={options}
                    value={selectedOptions}
                    getOptionLabel={(option) => option.label}
                    isOptionEqualToValue={(option, selectedOption) => option.dataKey === selectedOption.dataKey}
                    disabled={field.disabled || field.busy}
                    readOnly={field.readOnly || field.busy}
                    noOptionsText="Keine Anlagensätze verfügbar"
                    getOptionDisabled={(option) => (
                        effectiveMaxItems != null &&
                        selectedOptions.length >= effectiveMaxItems &&
                        !selectedDataKeys.includes(option.dataKey)
                    )}
                    onChange={(_, updatedOptions) => updateSelectedOptions(updatedOptions)}
                    renderOption={renderOption}
                    renderInput={(params) => renderInput(params, field)}
                    sx={props.controlSx}
                />
            )}
        </FormField>
    );
}

function createAttachmentSetOption(attachmentSet: ProcessNodeDefinitionMetadataForwardedAttachmentSet): AttachmentSetOption {
    const subLabelParts = [
        attachmentSet.dataKey,
        attachmentSet.subLabel,
        attachmentSet.isMultifile ? 'Mehrere Dateien' : 'Eine Datei',
        attachmentSet.origin.name,
    ].filter((part): part is string => part != null && part.trim().length > 0);

    return {
        dataKey: attachmentSet.dataKey,
        label: attachmentSet.label.trim().length > 0 ? attachmentSet.label : attachmentSet.dataKey,
        subLabel: subLabelParts.join(' - '),
    };
}

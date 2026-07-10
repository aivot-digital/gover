import {useMemo, type HTMLAttributes} from 'react';
import {BaseViewProps} from './base-view';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {
    ProcessInstanceAttachmentSetSelectElement,
} from '../models/elements/form/input/process-instance-attachment-set-select-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {Autocomplete, type AutocompleteRenderInputParams, Box, ListItemText, MenuItem, TextField} from '@mui/material';
import type {
    ProcessNodeDefinitionMetadataForwardedAttachmentSet,
} from '../modules/process/entities/process-node-definition-metadata';

interface AttachmentSetOption {
    dataKey: string;
    label: string;
    subLabel: string;
}

export function ProcessInstanceAttachmentSetSelectFieldView(props: BaseViewProps<ProcessInstanceAttachmentSetSelectElement, string[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

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

        for (const attachmentSet of opec?.incomingMetadata?.forwardedAttachmentSets ?? []) {
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
    }, [opec?.incomingMetadata?.forwardedAttachmentSets, selectedDataKeys]);

    const selectedOptions = useMemo(() => {
        return selectedDataKeys
            .map((dataKey) => optionsByDataKey.get(dataKey))
            .filter((option): option is AttachmentSetOption => option != null);
    }, [optionsByDataKey, selectedDataKeys]);

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const effectiveMaxItems = element.maxItems != null && element.maxItems > 0
        ? element.maxItems
        : undefined;

    const isSingleSelect = effectiveMaxItems === 1;

    const updateSelectedOptions = (updatedOptions: AttachmentSetOption[]) => {
        if (isBusy) {
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

        setValue(updatedDataKeys.length > 0 ? updatedDataKeys : null);
    };

    const renderHelperText = () => {
        const text = errors != null ? errors.join(' ') : element.hint;
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
                        color: errors != null ? 'error.main' : 'text.secondary',
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
        optionProps: HTMLAttributes<HTMLLIElement>,
        option: AttachmentSetOption,
    ) => (
        <MenuItem {...optionProps}>
            <ListItemText
                primary={option.label}
                secondary={option.subLabel}
            />
        </MenuItem>
    );

    const renderInput = (params: AutocompleteRenderInputParams) => (
        <TextField
            {...params}
            label={element.label ?? ''}
            placeholder={selectedOptions.length > 0 ? undefined : element.placeholder ?? undefined}
            error={errors != null}
            required={element.required ?? undefined}
            helperText={renderHelperText()}
            inputProps={{
                ...params.inputProps,
                readOnly: isBusy,
            }}
        />
    );

    if (isSingleSelect) {
        return (
            <Autocomplete<AttachmentSetOption, false, false, false>
                fullWidth
                options={options}
                value={selectedOptions[0] ?? null}
                getOptionLabel={(option) => option.label}
                isOptionEqualToValue={(option, selectedOption) => option.dataKey === selectedOption.dataKey}
                disabled={isDisabled}
                readOnly={isBusy}
                noOptionsText="Keine Anlagensätze verfügbar"
                onChange={(_, updatedOption) => {
                    updateSelectedOptions(updatedOption == null ? [] : [updatedOption]);
                }}
                renderOption={renderOption}
                renderInput={renderInput}
            />
        );
    }

    return (
        <Autocomplete<AttachmentSetOption, true, false, false>
            multiple
            fullWidth
            filterSelectedOptions
            options={options}
            value={selectedOptions}
            getOptionLabel={(option) => option.label}
            isOptionEqualToValue={(option, selectedOption) => option.dataKey === selectedOption.dataKey}
            disabled={isDisabled}
            readOnly={isBusy}
            noOptionsText="Keine Anlagensätze verfügbar"
            getOptionDisabled={(option) => (
                effectiveMaxItems != null &&
                selectedOptions.length >= effectiveMaxItems &&
                !selectedDataKeys.includes(option.dataKey)
            )}
            onChange={(_, updatedOptions) => {
                updateSelectedOptions(updatedOptions);
            }}
            renderOption={renderOption}
            renderInput={renderInput}
        />
    );
}

function createAttachmentSetOption(attachmentSet: ProcessNodeDefinitionMetadataForwardedAttachmentSet): AttachmentSetOption {
    const subLabelParts = [
        attachmentSet.dataKey,
        attachmentSet.subLabel,
        attachmentSet.origin.name,
    ].filter((part): part is string => part != null && part.trim().length > 0);

    return {
        dataKey: attachmentSet.dataKey,
        label: attachmentSet.label.trim().length > 0 ? attachmentSet.label : attachmentSet.dataKey,
        subLabel: subLabelParts.join(' - '),
    };
}

import {useMemo, type HTMLAttributes, type Key} from 'react';
import {Autocomplete, Box, Chip, ListItemText, TextField, type AutocompleteRenderInputParams} from '@mui/material';
import type {
    ProcessNodeDefinitionMetadataForwardedIdentity,
} from '../../modules/process/entities/process-node-definition-metadata';

interface IdentityOption {
    identityId: string;
    label: string;
    subLabel: string;
    available: boolean;
}

interface ProcessIdentitySelectProps {
    identities: ProcessNodeDefinitionMetadataForwardedIdentity[] | null | undefined;
    value: string[] | null | undefined;
    onChange: (value: string[] | null) => void;
    label: string;
    placeholder?: string | null;
    hint?: string | null;
    errors?: string[] | null;
    required?: boolean | null;
    disabled?: boolean | null;
    readOnly?: boolean | null;
    maxItems?: number | null;
}

export function ProcessIdentitySelect(props: ProcessIdentitySelectProps) {
    const {
        identities,
        value,
        onChange,
        label,
        placeholder,
        hint,
        errors,
        required,
        disabled,
        readOnly,
        maxItems,
    } = props;

    const metadataLoaded = identities != null;
    const selectedIdentityIds = useMemo(() => {
        return Array.from(new Set((value ?? [])
            .map((identityId) => identityId.trim())
            .filter((identityId) => identityId.length > 0)));
    }, [value]);

    const {
        options,
        optionsByIdentityId,
    } = useMemo(() => {
        const result = new Map<string, IdentityOption>();

        for (const identity of identities ?? []) {
            const identityId = identity.identityId.trim();
            if (identityId.length === 0 || result.has(identityId)) {
                continue;
            }

            result.set(identityId, createIdentityOption(identity, identityId));
        }

        return {
            options: Array.from(result.values()),
            optionsByIdentityId: result,
        };
    }, [identities]);

    const selectedOptions = useMemo(() => {
        return selectedIdentityIds.map((identityId) => optionsByIdentityId.get(identityId) ?? {
            identityId,
            label: identityId,
            subLabel: metadataLoaded ? 'Nicht mehr verfügbar' : identityId,
            available: !metadataLoaded,
        });
    }, [metadataLoaded, optionsByIdentityId, selectedIdentityIds]);

    const unavailableIdentityIds = useMemo(() => {
        if (!metadataLoaded) {
            return [];
        }

        return selectedIdentityIds.filter((identityId) => !optionsByIdentityId.has(identityId));
    }, [metadataLoaded, optionsByIdentityId, selectedIdentityIds]);

    const effectiveErrors = useMemo(() => {
        const result = new Set((errors ?? []).filter((error) => error.trim().length > 0));
        if (unavailableIdentityIds.length > 0) {
            const quotedIdentityIds = unavailableIdentityIds.map((identityId) => `„${identityId}“`).join(', ');
            result.add(unavailableIdentityIds.length === 1
                ? `Die ausgewählte Prozessidentität ${quotedIdentityIds} ist nicht mehr verfügbar.`
                : `Die ausgewählten Prozessidentitäten ${quotedIdentityIds} sind nicht mehr verfügbar.`);
        }
        return Array.from(result);
    }, [errors, unavailableIdentityIds]);

    const effectiveMaxItems = maxItems != null && maxItems > 0
        ? maxItems
        : undefined;
    const isSingleSelect = effectiveMaxItems === 1;

    const updateSelectedOptions = (updatedOptions: IdentityOption[]) => {
        if (readOnly) {
            return;
        }

        const updatedIdentityIds: string[] = [];
        const knownIdentityIds = new Set<string>();

        for (const option of updatedOptions) {
            if (knownIdentityIds.has(option.identityId)) {
                continue;
            }

            if (effectiveMaxItems != null && updatedIdentityIds.length >= effectiveMaxItems) {
                continue;
            }

            updatedIdentityIds.push(option.identityId);
            knownIdentityIds.add(option.identityId);
        }

        onChange(updatedIdentityIds.length > 0 ? updatedIdentityIds : null);
    };

    const renderHelperText = () => {
        const text = effectiveErrors.length > 0 ? effectiveErrors.join(' ') : hint;
        if ((text == null || text.length === 0) && effectiveMaxItems == null) {
            return undefined;
        }

        return (
            <Box
                component="span"
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
                        color: effectiveErrors.length > 0 ? 'error.main' : 'text.secondary',
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
        option: IdentityOption,
    ) => (
        <Box component="li" key={key} {...optionProps}>
            <ListItemText
                primary={option.label}
                secondary={option.subLabel}
            />
        </Box>
    );

    const renderInput = (params: AutocompleteRenderInputParams) => (
        <TextField
            {...params}
            label={label}
            placeholder={selectedOptions.length > 0 ? undefined : placeholder ?? undefined}
            error={effectiveErrors.length > 0}
            required={required ?? undefined}
            helperText={renderHelperText()}
            slotProps={{
                ...params.slotProps,
                htmlInput: {
                    ...params.slotProps.htmlInput,
                    readOnly: readOnly ?? undefined,
                },
            }}
        />
    );

    if (isSingleSelect) {
        return (
            <Autocomplete<IdentityOption, false, false, false>
                fullWidth
                options={options}
                value={selectedOptions[0] ?? null}
                getOptionLabel={(option) => option.label}
                isOptionEqualToValue={(option, selectedOption) => option.identityId === selectedOption.identityId}
                disabled={disabled ?? undefined}
                readOnly={readOnly ?? undefined}
                noOptionsText="Keine Prozessidentitäten verfügbar"
                onChange={(_, updatedOption) => {
                    updateSelectedOptions(updatedOption == null ? [] : [updatedOption]);
                }}
                renderOption={renderOption}
                renderInput={renderInput}
            />
        );
    }

    return (
        <Autocomplete<IdentityOption, true, false, false>
            multiple
            fullWidth
            filterSelectedOptions
            options={options}
            value={selectedOptions}
            getOptionLabel={(option) => option.label}
            isOptionEqualToValue={(option, selectedOption) => option.identityId === selectedOption.identityId}
            disabled={disabled ?? undefined}
            readOnly={readOnly ?? undefined}
            noOptionsText="Keine Prozessidentitäten verfügbar"
            getOptionDisabled={(option) => (
                effectiveMaxItems != null &&
                selectedOptions.length >= effectiveMaxItems &&
                !selectedIdentityIds.includes(option.identityId)
            )}
            onChange={(_, updatedOptions) => {
                updateSelectedOptions(updatedOptions);
            }}
            renderValue={(selected, getItemProps) => selected.map((option, index) => {
                const {key, ...itemProps} = getItemProps({index});
                return (
                    <Chip
                        key={key}
                        label={option.label}
                        color={option.available ? 'default' : 'error'}
                        {...itemProps}
                    />
                );
            })}
            renderOption={renderOption}
            renderInput={renderInput}
        />
    );
}

function createIdentityOption(
    identity: ProcessNodeDefinitionMetadataForwardedIdentity,
    identityId: string,
): IdentityOption {
    const subLabelParts = [
        identityId,
        identity.subLabel,
        identity.origin.name,
    ]
        .map((part) => part?.trim())
        .filter((part): part is string => part != null && part.length > 0);
    const normalizedLabel = identity.label.trim();

    return {
        identityId,
        label: normalizedLabel.length > 0 ? normalizedLabel : identityId,
        subLabel: subLabelParts.join(' - '),
        available: true,
    };
}

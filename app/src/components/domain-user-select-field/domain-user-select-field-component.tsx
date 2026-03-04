import {Autocomplete, Box, Chip, CircularProgress, TextField, Typography} from '@mui/material';
import {SyntheticEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import CheckIcon from '@mui/icons-material/Check';
import {
    createDomainAndUserSelectValueKey,
    DomainAndUserSelectOption,
    DomainAndUserSelectOptionConstraint,
    formatDomainAndUserSelectValue,
    loadDomainAndUserSelectOptions,
    normalizeDomainAndUserSelectItem,
} from './domain-user-select-options';
import {
    DomainAndUserSelectItem,
    DomainAndUserSelectItemType,
    DomainAndUserSelectItemTypes,
    DomainAndUserSelectProcessAccessConstraint,
} from '../../models/elements/form/input/domain-user-select-field-element';

interface DomainUserSelectFieldComponentProps {
    label: string;
    value: DomainAndUserSelectItem[] | null | undefined;
    onChange: (value: DomainAndUserSelectItem[] | null | undefined) => void;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    readOnly?: boolean;
    options?: DomainAndUserSelectOption[];
    allowedTypes?: DomainAndUserSelectItemType[] | null;
    processAccessConstraint?: DomainAndUserSelectProcessAccessConstraint | null;
}

function normalizeValues(values: Array<DomainAndUserSelectItem | unknown>): DomainAndUserSelectItem[] | undefined {
    const normalizedValues = values
        .map(normalizeDomainAndUserSelectItem)
        .filter((entry): entry is DomainAndUserSelectItem => entry != null);

    if (normalizedValues.length === 0) {
        return undefined;
    }

    const uniqueValues = new Map<string, DomainAndUserSelectItem>();
    for (const value of normalizedValues) {
        uniqueValues.set(createDomainAndUserSelectValueKey(value), value);
    }

    return Array.from(uniqueValues.values());
}

export function DomainUserSelectFieldComponent(props: DomainUserSelectFieldComponentProps) {
    const {
        label,
        value,
        onChange,
        placeholder,
        hint,
        error,
        disabled,
        required,
        readOnly,
        options: providedOptions,
        allowedTypes,
        processAccessConstraint,
    } = props;

    const [loadedOptions, setLoadedOptions] = useState<DomainAndUserSelectOption[]>([]);
    const [loadedOptionsKey, setLoadedOptionsKey] = useState<string>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();
    const [open, setOpen] = useState(false);
    const latestLoadTokenRef = useRef(0);
    const inFlightLoadKeysRef = useRef(new Set<string>());

    const allowedTypeSet = useMemo(() => {
        if (allowedTypes == null) {
            return new Set(DomainAndUserSelectItemTypes);
        }

        return new Set(allowedTypes);
    }, [allowedTypes]);

    const hasEnabledType = allowedTypeSet.size > 0;

    const optionConstraint = useMemo<DomainAndUserSelectOptionConstraint | undefined>(() => {
        const processId = processAccessConstraint?.processId;
        const processVersion = processAccessConstraint?.processVersion;
        if (processId == null || processVersion == null) {
            return undefined;
        }

        return {
            processId,
            processVersion,
            requiredPermissions: Array.from(new Set(
                (processAccessConstraint?.requiredPermissions ?? [])
                    .map((permission) => permission.trim())
                    .filter((permission) => permission.length > 0),
            ))
                .sort((a, b) => a.localeCompare(b, 'de')),
        };
    }, [processAccessConstraint]);

    const optionsLoadKey = useMemo(() => {
        if (optionConstraint == null) {
            return 'default';
        }

        return `${optionConstraint.processId}:${optionConstraint.processVersion}:${(optionConstraint.requiredPermissions ?? []).join('|')}`;
    }, [optionConstraint]);

    const options = providedOptions ?? loadedOptions;
    const loadedOptionKeys = useMemo(() => {
        return new Set(options.map((entry) => entry.key));
    }, [options]);

    const hasUnresolvedSelectedValues = useMemo(() => {
        if ((value ?? []).length === 0) {
            return false;
        }

        return (value ?? [])
            .map((entry) => normalizeDomainAndUserSelectItem(entry))
            .filter((entry): entry is DomainAndUserSelectItem => entry != null)
            .map((entry) => createDomainAndUserSelectValueKey(entry))
            .some((key) => !loadedOptionKeys.has(key));
    }, [loadedOptionKeys, value]);

    useEffect(() => {
        if (providedOptions != null) {
            latestLoadTokenRef.current += 1;
            inFlightLoadKeysRef.current.clear();
            setLoadedOptions(providedOptions);
            setLoadedOptionsKey('provided');
            setLoadError(undefined);
            setIsLoading(false);
        }
    }, [providedOptions]);

    const triggerLoad = useCallback(() => {
        if (providedOptions != null) {
            return;
        }

        if (!hasEnabledType || loadedOptionsKey === optionsLoadKey) {
            return;
        }

        if (inFlightLoadKeysRef.current.has(optionsLoadKey)) {
            return;
        }

        const loadToken = latestLoadTokenRef.current + 1;
        latestLoadTokenRef.current = loadToken;
        inFlightLoadKeysRef.current.add(optionsLoadKey);
        setIsLoading(true);
        setLoadError(undefined);

        loadDomainAndUserSelectOptions(false, optionConstraint)
            .then((nextOptions) => {
                if (latestLoadTokenRef.current !== loadToken) {
                    return;
                }

                setLoadedOptions(nextOptions);
                setLoadedOptionsKey(optionsLoadKey);
            })
            .catch(() => {
                if (latestLoadTokenRef.current !== loadToken) {
                    return;
                }

                setLoadedOptions([]);
                setLoadedOptionsKey(optionsLoadKey);
                setLoadError('Die Auswahloptionen konnten nicht geladen werden.');
            })
            .finally(() => {
                inFlightLoadKeysRef.current.delete(optionsLoadKey);

                if (latestLoadTokenRef.current === loadToken) {
                    setIsLoading(false);
                }
            });
    }, [hasEnabledType, loadedOptionsKey, optionConstraint, optionsLoadKey, providedOptions]);

    useEffect(() => {
        if (providedOptions != null) {
            return;
        }

        const optionsForCurrentKeyLoaded = loadedOptionsKey === optionsLoadKey;
        const shouldLoadOnOpen = open && !optionsForCurrentKeyLoaded;
        const shouldLoadForChipHydration = !open && hasUnresolvedSelectedValues && !optionsForCurrentKeyLoaded;
        if (!shouldLoadOnOpen && !shouldLoadForChipHydration) {
            return;
        }

        triggerLoad();
    }, [hasUnresolvedSelectedValues, loadedOptionsKey, open, optionsLoadKey, providedOptions, triggerLoad]);

    const selectableOptions = useMemo(() => {
        return options.filter((option) => {
            return allowedTypeSet.has(option.value.type);
        });
    }, [allowedTypeSet, options]);

    const optionLookup = useMemo(() => {
        return new Map(options.map((option) => [option.key, option]));
    }, [options]);

    const selectedOptions = useMemo(() => {
        return (value ?? []).map((entry) => {
            const normalizedEntry = normalizeDomainAndUserSelectItem(entry);
            if (normalizedEntry == null) {
                return null;
            }

            const key = createDomainAndUserSelectValueKey(normalizedEntry);
            return optionLookup.get(key) ?? {
                key,
                value: normalizedEntry,
                label: formatDomainAndUserSelectValue(normalizedEntry),
                subLabel: undefined,
                icon: undefined,
                group: 'Ausgewählt' as const,
            };
        });
    }, [optionLookup, value])
        .filter((entry): entry is DomainAndUserSelectOption => entry != null);

    return (
        <Autocomplete
            multiple
            disableCloseOnSelect
            open={open}
            onOpen={() => {
                setOpen(true);
            }}
            onClose={() => {
                setOpen(false);
            }}
            options={selectableOptions}
            loading={open && isLoading}
            readOnly={readOnly}
            disabled={disabled}
            value={selectedOptions}
            isOptionEqualToValue={(option, selectedOption) => option.key === selectedOption.key}
            groupBy={(option) => option.group}
            renderGroup={(params) => (
                <li key={params.key}>
                    <Box
                        sx={{
                            mt: 0.5,
                            mb: 0.25,
                            px: 1.5,
                            py: 0.375,
                            bgcolor: (theme) => theme.palette.action.hover,
                        }}
                    >
                        <Typography
                            variant="caption"
                            sx={{
                                fontWeight: 600,
                                lineHeight: 1.2,
                                color: 'text.secondary',
                            }}
                        >
                            {params.group}
                        </Typography>
                    </Box>
                    {params.children}
                </li>
            )}
            getOptionLabel={(option) => option.label}
            noOptionsText={
                isLoading
                    ? 'Lade Optionen…'
                    : !hasEnabledType
                        ? 'Keine Typen freigegeben'
                        : 'Keine Optionen verfügbar'
            }
            onChange={(_: SyntheticEvent, nextValues) => {
                onChange(normalizeValues(nextValues.map((entry) => entry.value)));
            }}
            renderTags={(tagValue, getTagProps) => {
                return tagValue.map((option, index) => (
                    <Chip
                        {...getTagProps({index})}
                        key={option.key}
                        icon={option.icon}
                        label={option.label}
                    />
                ));
            }}
            renderOption={(optionProps, option, state) => (
                <Box
                    component="li"
                    {...optionProps}
                    sx={{
                        py: 0.5,
                        minHeight: 40,
                    }}
                >
                    {
                        option.icon != null &&
                        <Box
                            sx={{
                                mr: 1,
                                display: 'flex',
                                alignItems: 'center',
                            }}
                        >
                            {option.icon}
                        </Box>
                    }

                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 0.125,
                            flex: 1,
                        }}
                    >
                        <Typography
                            variant="body2"
                            sx={{
                                lineHeight: 1.2,
                            }}
                        >
                            {option.label}
                        </Typography>
                        {
                            option.subLabel != null &&
                            <Typography
                                variant="caption"
                                color="textSecondary"
                                sx={{
                                    lineHeight: 1.2,
                                }}
                            >
                                {option.subLabel}
                            </Typography>
                        }
                    </Box>

                    <CheckIcon
                        sx={{
                            ml: 1,
                            fontSize: 18,
                            color: 'primary.main',
                            opacity: state.selected ? 1 : 0,
                        }}
                    />
                </Box>
            )}
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={label}
                    placeholder={placeholder}
                    required={required}
                    error={error != null}
                    helperText={error ?? loadError ?? hint}
                    InputLabelProps={{
                        title: label,
                    }}
                    InputProps={{
                        ...params.InputProps,
                        endAdornment: (
                            <>
                                {
                                    open && isLoading &&
                                    <CircularProgress
                                        color="inherit"
                                        size={16}
                                        sx={{mr: 1}}
                                    />
                                }
                                {params.InputProps.endAdornment}
                            </>
                        ),
                    }}
                />
            )}
        />
    );
}

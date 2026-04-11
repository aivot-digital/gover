import {Autocomplete, Box, CircularProgress, TextField, Typography} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import {SyntheticEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
    DataObjectSelectOption,
    formatDataObjectSelectValue,
    formatDataObjectSelectSubLabel,
    loadDataObjectSelectOptions,
    normalizeDataLabelAttributeKey,
    normalizeDataObjectId,
} from './data-object-select-options';
import {normalizeDataModelKey} from '../data-model-select-field/data-model-select-options';

export interface DataObjectSelectFieldComponentProps {
    label: string;
    value: string | null | undefined;
    onChange: (value: string | null | undefined) => void;
    dataModelKey: string | null | undefined;
    dataLabelAttributeKey?: string | null | undefined;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    readOnly?: boolean;
    options?: DataObjectSelectOption[];
}

export function DataObjectSelectFieldComponent(props: DataObjectSelectFieldComponentProps) {
    const {
        label,
        value,
        onChange,
        dataModelKey,
        dataLabelAttributeKey,
        placeholder,
        hint,
        error,
        disabled,
        required,
        readOnly,
        options: providedOptions,
    } = props;

    const [loadedOptions, setLoadedOptions] = useState<DataObjectSelectOption[]>([]);
    const [loadedOptionsKey, setLoadedOptionsKey] = useState<string>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();
    const [open, setOpen] = useState(false);
    const latestLoadTokenRef = useRef(0);
    const inFlightLoadKeysRef = useRef(new Set<string>());

    const normalizedDataModelKey = normalizeDataModelKey(dataModelKey);
    const normalizedDataLabelAttributeKey = normalizeDataLabelAttributeKey(dataLabelAttributeKey);
    const normalizedValue = normalizeDataObjectId(value);
    const optionsLoadKey = normalizedDataModelKey == null
        ? undefined
        : `${normalizedDataModelKey}:${normalizedDataLabelAttributeKey ?? ''}`;

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

    useEffect(() => {
        if (providedOptions != null) {
            return;
        }

        if (optionsLoadKey != null) {
            return;
        }

        latestLoadTokenRef.current += 1;
        inFlightLoadKeysRef.current.clear();
        setLoadedOptions([]);
        setLoadedOptionsKey(undefined);
        setLoadError(undefined);
        setIsLoading(false);
    }, [optionsLoadKey, providedOptions]);

    const options = useMemo(() => {
        if (providedOptions != null) {
            return providedOptions;
        }

        if (optionsLoadKey == null || loadedOptionsKey !== optionsLoadKey) {
            return [];
        }

        return loadedOptions;
    }, [loadedOptions, loadedOptionsKey, optionsLoadKey, providedOptions]);

    const optionLookup = useMemo(() => {
        return new Map(options.map((option) => [option.key, option]));
    }, [options]);

    const hasUnresolvedSelectedValue = normalizedValue != null && !optionLookup.has(normalizedValue);

    const triggerLoad = useCallback(() => {
        if (providedOptions != null || optionsLoadKey == null) {
            return;
        }

        if (loadedOptionsKey === optionsLoadKey) {
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

        loadDataObjectSelectOptions(normalizedDataModelKey!, normalizedDataLabelAttributeKey, false)
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
    }, [loadedOptionsKey, normalizedDataLabelAttributeKey, normalizedDataModelKey, optionsLoadKey, providedOptions]);

    useEffect(() => {
        if (providedOptions != null || optionsLoadKey == null) {
            return;
        }

        const optionsLoaded = loadedOptionsKey === optionsLoadKey;
        const shouldLoadOnOpen = open && !optionsLoaded;
        const shouldLoadForValueHydration = !open && hasUnresolvedSelectedValue && !optionsLoaded;
        if (!shouldLoadOnOpen && !shouldLoadForValueHydration) {
            return;
        }

        triggerLoad();
    }, [hasUnresolvedSelectedValue, loadedOptionsKey, open, optionsLoadKey, providedOptions, triggerLoad]);

    const selectedOption = useMemo(() => {
        if (normalizedValue == null) {
            return null;
        }

        return optionLookup.get(normalizedValue) ?? {
            key: normalizedValue,
            value: normalizedValue,
            label: formatDataObjectSelectValue(normalizedValue),
            subLabel: normalizedDataModelKey != null
                ? formatDataObjectSelectSubLabel(normalizedDataModelKey, normalizedValue)
                : normalizedValue,
        };
    }, [normalizedDataModelKey, normalizedValue, optionLookup]);

    return (
        <Autocomplete
            open={open}
            onOpen={() => {
                setOpen(true);
            }}
            onClose={() => {
                setOpen(false);
            }}
            options={options}
            loading={open && isLoading}
            readOnly={readOnly}
            disabled={disabled}
            value={selectedOption}
            isOptionEqualToValue={(option, selectedOption) => option.key === selectedOption.key}
            getOptionLabel={(option) => option.label}
            noOptionsText={
                optionsLoadKey == null
                    ? 'Bitte zuerst ein Datenmodell konfigurieren.'
                    : isLoading
                        ? 'Lade Optionen…'
                        : 'Keine Optionen verfügbar'
            }
            onChange={(_: SyntheticEvent, nextValue) => {
                onChange(nextValue?.value);
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

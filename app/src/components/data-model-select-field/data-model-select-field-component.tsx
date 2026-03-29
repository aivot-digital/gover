import {Autocomplete, Box, CircularProgress, TextField, Typography} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import {SyntheticEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
    DataModelSelectOption,
    formatDataModelSelectValue,
    loadDataModelSelectOptions,
    normalizeDataModelKey,
} from './data-model-select-options';

export interface DataModelSelectFieldComponentProps {
    label: string;
    value: string | null | undefined;
    onChange: (value: string | null | undefined) => void;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    readOnly?: boolean;
    options?: DataModelSelectOption[];
}

const OPTIONS_LOAD_KEY = 'default';

export function DataModelSelectFieldComponent(props: DataModelSelectFieldComponentProps) {
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
    } = props;

    const [loadedOptions, setLoadedOptions] = useState<DataModelSelectOption[]>([]);
    const [loadedOptionsKey, setLoadedOptionsKey] = useState<string>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();
    const [open, setOpen] = useState(false);
    const latestLoadTokenRef = useRef(0);
    const inFlightLoadKeysRef = useRef(new Set<string>());

    const options = providedOptions ?? loadedOptions;
    const optionLookup = useMemo(() => {
        return new Map(options.map((option) => [option.key, option]));
    }, [options]);

    const normalizedValue = normalizeDataModelKey(value);
    const hasUnresolvedSelectedValue = normalizedValue != null && !optionLookup.has(normalizedValue);

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

        if (loadedOptionsKey === OPTIONS_LOAD_KEY) {
            return;
        }

        if (inFlightLoadKeysRef.current.has(OPTIONS_LOAD_KEY)) {
            return;
        }

        const loadToken = latestLoadTokenRef.current + 1;
        latestLoadTokenRef.current = loadToken;
        inFlightLoadKeysRef.current.add(OPTIONS_LOAD_KEY);
        setIsLoading(true);
        setLoadError(undefined);

        loadDataModelSelectOptions(false)
            .then((nextOptions) => {
                if (latestLoadTokenRef.current !== loadToken) {
                    return;
                }

                setLoadedOptions(nextOptions);
                setLoadedOptionsKey(OPTIONS_LOAD_KEY);
            })
            .catch(() => {
                if (latestLoadTokenRef.current !== loadToken) {
                    return;
                }

                setLoadedOptions([]);
                setLoadedOptionsKey(OPTIONS_LOAD_KEY);
                setLoadError('Die Auswahloptionen konnten nicht geladen werden.');
            })
            .finally(() => {
                inFlightLoadKeysRef.current.delete(OPTIONS_LOAD_KEY);

                if (latestLoadTokenRef.current === loadToken) {
                    setIsLoading(false);
                }
            });
    }, [loadedOptionsKey, providedOptions]);

    useEffect(() => {
        if (providedOptions != null) {
            return;
        }

        const optionsLoaded = loadedOptionsKey === OPTIONS_LOAD_KEY;
        const shouldLoadOnOpen = open && !optionsLoaded;
        const shouldLoadForValueHydration = !open && hasUnresolvedSelectedValue && !optionsLoaded;
        if (!shouldLoadOnOpen && !shouldLoadForValueHydration) {
            return;
        }

        triggerLoad();
    }, [hasUnresolvedSelectedValue, loadedOptionsKey, open, providedOptions, triggerLoad]);

    const selectedOption = useMemo(() => {
        if (normalizedValue == null) {
            return null;
        }

        return optionLookup.get(normalizedValue) ?? {
            key: normalizedValue,
            value: normalizedValue,
            label: formatDataModelSelectValue(normalizedValue),
        };
    }, [normalizedValue, optionLookup]);

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
            noOptionsText={isLoading ? 'Lade Optionen…' : 'Keine Optionen verfügbar'}
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

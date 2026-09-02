import {
    Autocomplete,
    Box,
    Chip,
    CircularProgress,
    type SxProps,
    TextField,
    type TextFieldProps,
    type Theme,
    Typography,
} from '@mui/material';
import {SyntheticEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import CheckIcon from '@aivot/mui-material-symbols-400-n25-outlined/Check';
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
import {
    FormField,
    type FormFieldControlContext,
    type FormFieldLayoutProps,
    getNativeInputAriaProps,
} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

export interface DomainUserSelectFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    value: DomainAndUserSelectItem[] | null | undefined;
    onChange: (value: DomainAndUserSelectItem[] | null) => void;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    busy?: boolean;
    required?: boolean;
    readOnly?: boolean;
    options?: DomainAndUserSelectOption[];
    onOptionsChange?: (options: DomainAndUserSelectOption[]) => void;
    allowedTypes?: DomainAndUserSelectItemType[] | null;
    processAccessConstraint?: DomainAndUserSelectProcessAccessConstraint | null;
    size?: TextFieldProps['size'];
    controlSx?: SxProps<Theme>;
}

function normalizeValues(values: Array<DomainAndUserSelectItem | unknown>): DomainAndUserSelectItem[] | null {
    const normalizedValues = values
        .map(normalizeDomainAndUserSelectItem)
        .filter((entry): entry is DomainAndUserSelectItem => entry != null);

    if (normalizedValues.length === 0) {
        return null;
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
        busy,
        required,
        readOnly,
        options: providedOptions,
        onOptionsChange,
        allowedTypes,
        processAccessConstraint,
        size = 'small',
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
    useEffect(() => {
        onOptionsChange?.(options);
    }, [onOptionsChange, options]);

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

    const triggerLoad = useCallback((forceReload: boolean = false) => {
        if (providedOptions != null) {
            return;
        }

        if (!hasEnabledType || (!forceReload && loadedOptionsKey === optionsLoadKey)) {
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

        loadDomainAndUserSelectOptions(forceReload, optionConstraint)
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

    const effectiveError = error ?? loadError;

    return (
        <FormField
            id={props.id}
            label={label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={hint}
            error={effectiveError}
            required={required}
            disabled={disabled}
            readOnly={readOnly}
            busy={busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(fieldContext: FormFieldControlContext) => (
                <Autocomplete
                    id={fieldContext.controlId}
                    multiple
                    disableCloseOnSelect
                    fullWidth
                    size={size}
                    open={open}
                    onOpen={() => {
                        if (!busy) {
                            setOpen(true);
                            triggerLoad(optionConstraint != null);
                        }
                    }}
                    onClose={() => {
                        setOpen(false);
                    }}
                    options={selectableOptions}
                    loading={open && isLoading}
                    readOnly={readOnly || busy}
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
                        if (!busy) {
                            onChange(normalizeValues(nextValues.map((entry) => entry.value)));
                        }
                    }}
                    renderValue={(tagValue, getItemProps) => {
                        return tagValue.map((option, index) => {
                            const {key, ...itemProps} = getItemProps({index});

                            return (
                                <Chip
                                    {...itemProps}
                                    key={option.key || key}
                                    icon={option.icon}
                                    label={option.label}
                                    size={size}
                                    sx={{
                                        '& .MuiChip-icon': {
                                            width: size === 'small' ? 18 : 24,
                                            height: size === 'small' ? 18 : 24,
                                            fontSize: size === 'small' ? 18 : 24,
                                        },
                                    }}
                                />
                            );
                        });
                    }}
                    renderOption={({key, ...optionProps}, option, state) => (
                        <Box
                            key={key}
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
                    renderInput={(params) => {
                        const nativeAriaProps = getNativeInputAriaProps(
                            fieldContext,
                            params.slotProps.htmlInput,
                        );

                        return (
                            <TextField
                                {...params}
                                id={fieldContext.controlId}
                                label={undefined}
                                placeholder={placeholder}
                                required={required}
                                error={fieldContext.invalid}
                                helperText={undefined}
                                fullWidth
                                margin="none"
                                size={size}
                                slotProps={{
                                    ...params.slotProps,
                                    input: {
                                        ...params.slotProps.input,
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
                                                {params.slotProps.input.endAdornment}
                                            </>
                                        ),
                                    },
                                    htmlInput: {
                                        ...params.slotProps.htmlInput,
                                        ...nativeAriaProps,
                                        'aria-busy': isLoading || nativeAriaProps['aria-busy'] || undefined,
                                    },
                                }}
                            />
                        );
                    }}
                    sx={[
                        {
                            cursor: busy ? 'not-allowed' : undefined,
                            '& .MuiInputBase-root': {
                                minHeight: FormFieldTokens.controlMinHeight,
                                backgroundColor: busy ? getDisabledFieldBackground : undefined,
                            },
                        },
                        ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                    ]}
                />
            )}
        </FormField>
    );
}

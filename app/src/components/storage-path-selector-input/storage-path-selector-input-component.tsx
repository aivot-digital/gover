import {
    Autocomplete,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Grid,
    IconButton,
    InputAdornment,
    Stack,
    TextField,
    Tooltip,
    Typography,
    type SxProps,
    type Theme,
} from '@mui/material';
import CheckIcon from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import CloseIcon from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import FolderIcon from '@aivot/mui-material-symbols-400-n25-outlined/Folder';
import StorageIcon from '@aivot/mui-material-symbols-400-n25-outlined/Storage';
import {type SyntheticEvent, useEffect, useMemo, useRef, useState} from 'react';
import {
    type StoragePathSelectorInputElementValue,
} from '../../models/elements/form/input/storage-path-selector-input-element';
import {
    StorageProviderType,
    StorageProviderTypeLabels,
    StorageProviderTypes,
} from '../../modules/storage/enums/storage-provider-type';
import {StorageProvidersApiService} from '../../modules/storage/storage-providers-api-service';
import {type StorageProviderEntity} from '../../modules/storage/entities/storage-provider-entity';
import {StorageExplorer} from '../../modules/storage/components/storage-explorer';
import {
    FormField,
    FormFieldGroup,
    type FormFieldGroupLayoutProps,
    getNativeInputAriaProps,
} from '../form-field';
import {formFieldInputRootSx} from '../../theming/form-field-tokens';

export interface StoragePathSelectorInputComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: StoragePathSelectorInputElementValue | null;
    onChange: (value: StoragePathSelectorInputElementValue | null) => void;
    allowedStorageProviderTypes?: StorageProviderType[] | null;
    storageProviderSelectHint?: string | null;
    placeholder?: string | null;
    hint?: string;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    allowReadOnlyStorageProviders?: boolean;
    controlSx?: SxProps<Theme>;
}

interface StorageProviderOption {
    id: number;
    name: string;
    description?: string | null;
    type?: StorageProviderType | null;
    readOnlyStorage?: boolean;
}

const ROOT_PATH = '/';

function normalizeDirectoryPath(path: string | null | undefined): string | null {
    if (path == null || path.trim().length === 0) {
        return null;
    }

    if (path === ROOT_PATH) {
        return ROOT_PATH;
    }

    let normalized = path.trim();
    if (!normalized.startsWith('/')) {
        normalized = `/${normalized}`;
    }

    if (!normalized.endsWith('/')) {
        normalized = `${normalized}/`;
    }

    return normalized;
}

function normalizeTypedPath(path: string | null | undefined): string | null {
    if (path == null || path.trim().length === 0) {
        return null;
    }

    return path.trim();
}

function containsTemplateTag(path: string | null | undefined): boolean {
    return path?.includes('{{') === true ||
        path?.includes('{%') === true ||
        path?.includes('{!') === true ||
        path?.includes('{#') === true;
}

function normalizeAllowedTypes(types: StorageProviderType[] | null | undefined): StorageProviderType[] {
    return types ?? StorageProviderTypes;
}

function toOption(provider: StorageProviderEntity): StorageProviderOption {
    return {
        id: provider.id,
        name: provider.name,
        description: provider.description,
        type: provider.type,
        readOnlyStorage: provider.readOnlyStorage,
    };
}

export function StoragePathSelectorInputComponent(props: StoragePathSelectorInputComponentProps) {
    const {
        label,
        value,
        onChange,
        allowedStorageProviderTypes,
        storageProviderSelectHint,
        placeholder,
        hint,
        error,
        disabled,
        required,
        readOnly,
        busy,
        allowReadOnlyStorageProviders = false,
    } = props;

    const allowedTypes = useMemo(() => normalizeAllowedTypes(allowedStorageProviderTypes), [allowedStorageProviderTypes]);
    const allowedTypesKey = allowedTypes.join('|');
    const [providers, setProviders] = useState<StorageProviderOption[]>([]);
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);
    const [loadError, setLoadError] = useState<string>();
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const loadTokenRef = useRef(0);

    useEffect(() => {
        const loadToken = loadTokenRef.current + 1;
        loadTokenRef.current = loadToken;

        if (allowedTypes.length === 0) {
            setProviders([]);
            setLoadError(undefined);
            setIsLoadingProviders(false);
            return;
        }

        setIsLoadingProviders(true);
        setLoadError(undefined);

        const api = new StorageProvidersApiService();
        const request = allowedTypes.length === StorageProviderTypes.length
            ? api.listAll({
                readOnlyStorage: allowReadOnlyStorageProviders ? undefined : false,
            })
                .then((page) => page.content.filter((provider) => allowedTypes.includes(provider.type)))
            : Promise
                .all(allowedTypes.map((type) => api.listAll({
                    type,
                    readOnlyStorage: allowReadOnlyStorageProviders ? undefined : false,
                })))
                .then((pages) => pages.flatMap((page) => page.content));

        request
            .then((loadedProviders) => {
                if (loadTokenRef.current !== loadToken) {
                    return;
                }

                const uniqueProviders = Array
                    .from(new Map(loadedProviders.map((provider) => [provider.id, provider])).values())
                    .sort((a, b) => a.name.localeCompare(b.name, 'de'))
                    .map(toOption);

                setProviders(uniqueProviders);
            })
            .catch(() => {
                if (loadTokenRef.current !== loadToken) {
                    return;
                }

                setProviders([]);
                setLoadError('Die Speicheranbieter konnten nicht geladen werden.');
            })
            .finally(() => {
                if (loadTokenRef.current === loadToken) {
                    setIsLoadingProviders(false);
                }
            });
    }, [allowReadOnlyStorageProviders, allowedTypesKey]);

    const selectedProvider = useMemo<StorageProviderOption | null>(() => {
        if (value?.storageProviderId == null) {
            return null;
        }

        return providers.find((provider) => provider.id === value.storageProviderId) ?? {
            id: value.storageProviderId,
            name: `Speicheranbieter #${value.storageProviderId}`,
        };
    }, [providers, value?.storageProviderId]);

    const selectedPath = value?.path ?? null;
    const explorerPath = containsTemplateTag(selectedPath)
        ? ROOT_PATH
        : normalizeDirectoryPath(selectedPath) ?? ROOT_PATH;
    const isReadonlyOrDisabled = disabled === true || readOnly === true || busy === true;
    const selectedProviderIsReadOnlyDisabled = !allowReadOnlyStorageProviders && selectedProvider?.readOnlyStorage === true;
    const canBrowse = !isReadonlyOrDisabled && !selectedProviderIsReadOnlyDisabled && value?.storageProviderId != null;
    const resolvedError = error ?? (
        selectedProviderIsReadOnlyDisabled
            ? 'Dieser Speicheranbieter ist nur lesend und kann hier nicht ausgewählt werden.'
            : undefined
    );

    const handleProviderChange = (_: SyntheticEvent, provider: StorageProviderOption | null): void => {
        if (provider == null) {
            onChange(null);
            return;
        }

        if (!allowReadOnlyStorageProviders && provider.readOnlyStorage === true) {
            return;
        }

        onChange({
            storageProviderId: provider.id,
            path: provider.id === value?.storageProviderId ? selectedPath : null,
        });
    };

    const handlePathChange = (path: string): void => {
        if (value?.storageProviderId == null) {
            return;
        }

        onChange({
            storageProviderId: value.storageProviderId,
            path,
        });
    };

    const handlePathBlur = (): void => {
        if (value?.storageProviderId == null) {
            return;
        }

        onChange({
            storageProviderId: value.storageProviderId,
            path: normalizeTypedPath(selectedPath),
        });
    };

    const handleFolderSelect = (path: string): void => {
        if (value?.storageProviderId == null) {
            return;
        }

        onChange({
            storageProviderId: value.storageProviderId,
            path: normalizeDirectoryPath(path) ?? ROOT_PATH,
        });
        setIsDialogOpen(false);
    };

    return (
        <FormFieldGroup
            id={props.id}
            label={label}
            hint={hint}
            error={resolvedError}
            required={required}
            disabled={disabled}
            readOnly={readOnly}
            busy={busy}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            margin={props.margin}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(group) => (<>
                <Grid
                    container
                    spacing={1.5}
                    sx={props.controlSx}
                >
                    <Grid size={{xs: 12}}>
                        <FormField
                            label="Speicheranbieter"
                            hint={storageProviderSelectHint}
                            error={loadError}
                            required={required}
                            disabled={disabled}
                            readOnly={readOnly}
                            busy={busy || isLoadingProviders}
                            ariaDescribedBy={group.describedBy}
                            showOptionalIndicator={false}
                            margin="none"
                        >
                            {(providerField) => (
                                <Autocomplete
                                    id={providerField.controlId}
                                    options={providers}
                                    loading={isLoadingProviders}
                                    disabled={disabled || busy}
                                    readOnly={readOnly || busy}
                                    value={selectedProvider}
                                    isOptionEqualToValue={(option, selectedOption) => option.id === selectedOption.id}
                                    getOptionLabel={(option) => option.name}
                                    getOptionDisabled={(option) => !allowReadOnlyStorageProviders && option.readOnlyStorage === true}
                                    noOptionsText={allowedTypes.length === 0 ? 'Keine Speicheranbieter-Typen zugelassen' : 'Keine Speicheranbieter verfügbar'}
                                    onChange={handleProviderChange}
                                    sx={{
                                        '& .MuiInputBase-root': formFieldInputRootSx,
                                    }}
                                    renderOption={({key, ...optionProps}, option, state) => (
                                        <Box
                                            key={key}
                                            component="li"
                                            {...optionProps}
                                            sx={{py: 0.5, minHeight: 42}}
                                        >
                                            <StorageIcon sx={{mr: 1, fontSize: 20, color: 'text.secondary'}}/>
                                            <Box sx={{minWidth: 0, flex: 1}}>
                                                <Typography variant="body2" noWrap>
                                                    {option.name}
                                                </Typography>
                                                {option.type != null && (
                                                    <Typography variant="caption" noWrap sx={{color: 'text.secondary'}}>
                                                        {StorageProviderTypeLabels[option.type]}
                                                    </Typography>
                                                )}
                                            </Box>
                                            <CheckIcon
                                                sx={{ml: 1, fontSize: 18, color: 'primary.main', opacity: state.selected ? 1 : 0}}
                                            />
                                        </Box>
                                    )}
                                    renderInput={(params) => (
                                        <TextField
                                            {...params}
                                            size="small"
                                            margin="none"
                                            placeholder="Speicheranbieter auswählen"
                                            error={providerField.invalid}
                                            slotProps={{
                                                ...params.slotProps,
                                                htmlInput: {
                                                    ...params.slotProps.htmlInput,
                                                    ...getNativeInputAriaProps(
                                                        providerField,
                                                        params.slotProps.htmlInput,
                                                    ),
                                                },
                                                input: {
                                                    ...params.slotProps.input,
                                                    endAdornment: (
                                                        <>
                                                            {isLoadingProviders && (
                                                                <CircularProgress color="inherit" size={16} sx={{mr: 1}}/>
                                                            )}
                                                            {params.slotProps.input.endAdornment}
                                                        </>
                                                    ),
                                                },
                                            }}
                                        />
                                    )}
                                />
                            )}
                        </FormField>
                    </Grid>

                    <Grid size={{xs: 12}}>
                        <FormField
                            label="Pfad"
                            required={required}
                            disabled={disabled || selectedProviderIsReadOnlyDisabled || value?.storageProviderId == null}
                            readOnly={readOnly}
                            busy={busy}
                            error={resolvedError}
                            hideHelperText
                            ariaDescribedBy={group.describedBy}
                            showOptionalIndicator={false}
                            margin="none"
                        >
                            {(pathField) => (
                                <TextField
                                    id={pathField.controlId}
                                    size="small"
                                    margin="none"
                                    value={selectedPath ?? ''}
                                    onChange={(event) => handlePathChange(event.target.value)}
                                    onBlur={handlePathBlur}
                                    placeholder={placeholder ?? 'Ordner auswählen'}
                                    error={pathField.invalid}
                                    disabled={pathField.disabled || pathField.busy}
                                    fullWidth
                                    slotProps={{
                                        htmlInput: getNativeInputAriaProps(pathField),
                                        input: {
                                            readOnly: pathField.readOnly || pathField.busy,
                                            sx: formFieldInputRootSx,
                                            endAdornment: (
                                                <InputAdornment position="end">
                                                    {value != null && !isReadonlyOrDisabled && (
                                                        <Tooltip title="Auswahl löschen" arrow>
                                                            <IconButton
                                                                size="small"
                                                                aria-label={`${label}: Pfad löschen`}
                                                                onClick={() => onChange({
                                                                    storageProviderId: value.storageProviderId,
                                                                    path: null,
                                                                })}
                                                            >
                                                                <CloseIcon fontSize="small"/>
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}
                                                    <Tooltip
                                                        title={value?.storageProviderId == null ? 'Bitte zuerst einen Speicheranbieter auswählen' : 'Ordner auswählen'}
                                                        arrow
                                                    >
                                                        <span>
                                                            <IconButton
                                                                size="small"
                                                                aria-label={`${label}: Ordner auswählen`}
                                                                aria-haspopup="dialog"
                                                                aria-expanded={isDialogOpen}
                                                                disabled={!canBrowse}
                                                                onClick={() => setIsDialogOpen(true)}
                                                            >
                                                                <FolderIcon fontSize="small"/>
                                                            </IconButton>
                                                        </span>
                                                    </Tooltip>
                                                </InputAdornment>
                                            ),
                                        },
                                    }}
                                />
                            )}
                        </FormField>
                    </Grid>
                </Grid>

                <Dialog
                    open={isDialogOpen && value?.storageProviderId != null}
                    onClose={() => setIsDialogOpen(false)}
                    maxWidth="lg"
                    fullWidth
                >
                    <DialogTitle sx={{pr: 6}}>
                        <Stack sx={{minWidth: 0}}>
                            <Typography variant="subtitle1">Zielpfad auswählen</Typography>
                            {selectedProvider != null && (
                                <Typography variant="caption" noWrap sx={{color: 'text.secondary'}}>
                                    {selectedProvider.name}
                                </Typography>
                            )}
                        </Stack>
                        <IconButton
                            aria-label="Dialog schließen"
                            onClick={() => setIsDialogOpen(false)}
                            size="small"
                            sx={{position: 'absolute', right: 12, top: 12}}
                        >
                            <CloseIcon fontSize="small"/>
                        </IconButton>
                    </DialogTitle>

                    <DialogContent dividers>
                        {value?.storageProviderId != null && (
                            <StorageExplorer
                                providerId={value.storageProviderId}
                                initialPath={explorerPath}
                                onFolderSelect={handleFolderSelect}
                                folderSelectLabel="Diesen Ordner auswählen"
                                disableFileDialog
                                showTopNavigationBar
                                minGridHeight={480}
                            />
                        )}
                    </DialogContent>

                    <DialogActions sx={{pt: 2}}>
                        <Button onClick={() => setIsDialogOpen(false)}>
                            Schließen
                        </Button>
                    </DialogActions>
                </Dialog>
            </>)}
        </FormFieldGroup>
    );
}

import {
    CircularProgress,
    IconButton,
    InputAdornment,
    Paper,
    type SxProps,
    Stack,
    TextField,
    type Theme,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography
} from '@mui/material';
import MyLocationOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MyLocation';
import NearMeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/NearMe';
import SearchOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Search';
import ClearOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import LocationOnOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LocationOn';
import {
    type KeyboardEventHandler,
    type ReactNode,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react';
import {MapPointValue} from '../../models/elements/form/input/map-point-field-element';
import {
    LeafletPointPickerMap,
    type LeafletPoint,
    type LeafletPointPickerMapHandle
} from './leaflet-point-picker-map';
import {
    FormField,
    type FormFieldControlContext,
    FormFieldGroup,
    type FormFieldGroupContext,
    type FormFieldGroupLayoutProps,
    getNativeInputAriaProps,
} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

export interface MapPointFieldComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: MapPointValue | null;
    onChange: (value: MapPointValue | null) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    readOnly?: boolean;
    error?: string;
    zoom?: number;
    centerLatitude?: number;
    centerLongitude?: number;
    controlSx?: SxProps<Theme>;
}

const DEFAULT_CENTER = {
    lat: 52.52,
    lon: 13.405,
};

type InputMode = 'search' | 'coordinates';

interface NominatimAddress {
    road?: string;
    house_number?: string;
    postcode?: string;
    city?: string;
    town?: string;
    village?: string;
    municipality?: string;
    county?: string;
    state?: string;
    country?: string;
}

interface NominatimSearchEntry {
    lat: string;
    lon: string;
    display_name?: string;
    address?: NominatimAddress;
}

interface NominatimReverseResult {
    display_name?: string;
    address?: NominatimAddress;
}

interface MapPointTextInputProps {
    label: string;
    value: string;
    onChange: (value: string) => void;
    groupContext: FormFieldGroupContext;
    groupError?: string;
    placeholder?: string;
    endAdornment?: ReactNode;
    inputMode?: 'decimal' | 'search';
    disabled?: boolean;
    busy?: boolean;
    loading?: boolean;
    readOnly?: boolean;
    onKeyDown?: KeyboardEventHandler<HTMLInputElement>;
}

function MapPointTextInput(props: MapPointTextInputProps) {
    return (
        <FormField
            label={props.label}
            ariaDescribedBy={props.groupContext.describedBy}
            error={props.groupContext.invalid ? props.groupError : undefined}
            hideHelperText
            disabled={props.disabled}
            busy={props.busy}
            readOnly={props.readOnly}
            margin="none"
            showOptionalIndicator={false}
        >
            {(fieldContext: FormFieldControlContext) => (
                <TextField
                    id={fieldContext.controlId}
                    value={props.value}
                    onChange={(event) => {
                        if (!props.busy && !props.readOnly) {
                            props.onChange(event.target.value);
                        }
                    }}
                    onKeyDown={props.onKeyDown}
                    label={undefined}
                    placeholder={props.placeholder}
                    disabled={props.disabled}
                    error={fieldContext.invalid}
                    helperText={undefined}
                    fullWidth
                    margin="none"
                    size="small"
                    slotProps={{
                        input: {
                            readOnly: props.readOnly || props.busy,
                            endAdornment: props.endAdornment,
                            sx: {
                                minHeight: FormFieldTokens.controlMinHeight,
                                backgroundColor: props.busy ? getDisabledFieldBackground : undefined,
                                cursor: props.busy ? 'not-allowed' : undefined,
                            },
                        },
                        htmlInput: {
                            ...getNativeInputAriaProps(fieldContext),
                            'aria-busy': props.loading || fieldContext.ariaProps['aria-busy'] || undefined,
                            inputMode: props.inputMode,
                        },
                    }}
                />
            )}
        </FormField>
    );
}

function clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value));
}

function normalizeZoom(zoom?: number): number {
    if (zoom == null || Number.isNaN(zoom)) {
        return 14;
    }

    return clamp(Math.round(zoom), 1, 19);
}

function normalizePoint(value: MapPointValue): MapPointValue | null {
    const latitude = value.latitude ?? null;
    const longitude = value.longitude ?? null;
    const address = value.address?.trim().length ? value.address.trim() : null;

    if (latitude == null && longitude == null && address == null) {
        return null;
    }

    return {
        latitude,
        longitude,
        address,
    };
}

function parseCoordinateInput(input: string): number | undefined {
    const normalized = input.trim().replace(',', '.');
    if (normalized.length === 0) {
        return undefined;
    }

    const parsed = Number.parseFloat(normalized);
    if (Number.isNaN(parsed)) {
        return Number.NaN;
    }

    return parsed;
}

function formatNominatimAddress(address?: NominatimAddress, fallback?: string): string | undefined {
    if (address == null) {
        if (fallback == null || fallback.trim().length === 0) {
            return undefined;
        }

        return fallback
            .split(',')
            .map((part) => part.trim())
            .filter((part) => part.length > 0)
            .slice(0, 3)
            .join(', ');
    }

    const street = [address.road, address.house_number]
        .filter((part): part is string => part != null && part.trim().length > 0)
        .join(' ');
    const city = address.city ?? address.town ?? address.village ?? address.municipality ?? address.county;
    const locality = [address.postcode, city]
        .filter((part): part is string => part != null && part.trim().length > 0)
        .join(' ');
    const region = [address.state, address.country]
        .filter((part): part is string => part != null && part.trim().length > 0)
        .join(', ');
    const parts = [street, locality, region].filter((part) => part.length > 0);

    if (parts.length > 0) {
        return parts.join(', ');
    }

    if (fallback == null || fallback.trim().length === 0) {
        return undefined;
    }

    return fallback
        .split(',')
        .map((part) => part.trim())
        .filter((part) => part.length > 0)
        .slice(0, 3)
        .join(', ');
}

export function MapPointFieldComponent(props: MapPointFieldComponentProps) {
    const zoom = normalizeZoom(props.zoom);
    const [inputMode, setInputMode] = useState<InputMode>('search');
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const [isResolvingAddress, setIsResolvingAddress] = useState(false);
    const [addressResolveError, setAddressResolveError] = useState<string | undefined>(undefined);
    const mapRef = useRef<LeafletPointPickerMapHandle | null>(null);
    const reverseLookupRequestId = useRef(0);
    const [latitudeInput, setLatitudeInput] = useState('');
    const [longitudeInput, setLongitudeInput] = useState('');

    const hasCoordinates = props.value?.latitude != null && props.value?.longitude != null;
    const markerLat = props.value?.latitude ?? undefined;
    const markerLon = props.value?.longitude ?? undefined;
    const isMapDisabled = props.disabled === true || props.busy === true || props.readOnly === true;
    const hasPointValue = props.value != null && normalizePoint(props.value) != null;
    const canClearSearchInput = searchQuery.trim().length > 0 || (hasPointValue && props.required !== true);
    const clearSearchInputLabel = hasPointValue && props.required !== true ? 'Kartenpunkt-Auswahl löschen' : 'Suche löschen';

    const configuredCenter = useMemo(() => {
        if (props.centerLatitude != null && props.centerLongitude != null) {
            return {
                lat: props.centerLatitude,
                lon: props.centerLongitude,
            };
        }

        return DEFAULT_CENTER;
    }, [props.centerLatitude, props.centerLongitude]);

    const mapCenter = useMemo(() => {
        if (hasCoordinates && markerLat != null && markerLon != null) {
            return {
                lat: markerLat,
                lon: markerLon,
            };
        }

        return configuredCenter;
    }, [configuredCenter, hasCoordinates, markerLat, markerLon]);

    const mapMarker = useMemo<LeafletPoint | null>(() => {
        if (!hasCoordinates || markerLat == null || markerLon == null) {
            return null;
        }

        return {
            lat: markerLat,
            lon: markerLon,
        };
    }, [hasCoordinates, markerLat, markerLon]);

    useEffect(() => {
        setLatitudeInput(props.value?.latitude != null ? props.value.latitude.toFixed(6) : '');
    }, [props.value?.latitude]);

    useEffect(() => {
        setLongitudeInput(props.value?.longitude != null ? props.value.longitude.toFixed(6) : '');
    }, [props.value?.longitude]);

    const updatePoint = useCallback((latitude: number, longitude: number, options?: {
        address?: string;
        panToMap?: boolean;
    }) => {
        props.onChange(normalizePoint({
            latitude,
            longitude,
            address: options?.address ?? props.value?.address,
        }));

        if (options?.panToMap !== false) {
            mapRef.current?.panTo({
                lat: latitude,
                lon: longitude,
            });
        }
    }, [props.onChange, props.value?.address]);

    const resolveAddressForPoint = useCallback(async (latitude: number, longitude: number) => {
        const requestId = reverseLookupRequestId.current + 1;
        reverseLookupRequestId.current = requestId;
        setIsResolvingAddress(true);
        setAddressResolveError(undefined);

        try {
            const response = await window.fetch(
                `https://nominatim.openstreetmap.org/reverse?format=jsonv2&addressdetails=1&accept-language=de&lat=${encodeURIComponent(String(latitude))}&lon=${encodeURIComponent(String(longitude))}`,
            );

            if (!response.ok) {
                throw new Error('reverse_geocoding_failed');
            }

            const result = await response.json() as NominatimReverseResult;
            if (reverseLookupRequestId.current !== requestId) {
                return;
            }

            const formattedAddress = formatNominatimAddress(result.address, result.display_name);
            updatePoint(latitude, longitude, {
                address: formattedAddress,
                panToMap: false,
            });
        } catch {
            if (reverseLookupRequestId.current !== requestId) {
                return;
            }

            setAddressResolveError('Adresse konnte nicht automatisch ermittelt werden.');
        } finally {
            if (reverseLookupRequestId.current === requestId) {
                setIsResolvingAddress(false);
            }
        }
    }, [updatePoint]);

    const handleMapPick = useCallback((point: LeafletPoint) => {
        updatePoint(point.lat, point.lon);
        void resolveAddressForPoint(point.lat, point.lon);
    }, [resolveAddressForPoint, updatePoint]);

    const handleSearch = async () => {
        const query = searchQuery.trim();
        if (query.length < 3) {
            setAddressResolveError('Bitte geben Sie mindestens 3 Zeichen für die Suche ein.');
            return;
        }

        try {
            setIsSearching(true);
            setAddressResolveError(undefined);
            const response = await window.fetch(
                `https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&accept-language=de&limit=1&q=${encodeURIComponent(query)}`,
            );

            if (!response.ok) {
                setAddressResolveError('Suche nach Ort oder Adresse ist aktuell nicht verfügbar.');
                return;
            }

            const entries = await response.json() as NominatimSearchEntry[];
            const first = entries?.[0];
            if (first == null) {
                setAddressResolveError('Kein passender Ort gefunden.');
                return;
            }

            const latitude = Number.parseFloat(first.lat);
            const longitude = Number.parseFloat(first.lon);
            if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
                setAddressResolveError('Die gefundenen Koordinaten sind ungültig.');
                return;
            }

            updatePoint(latitude, longitude, {
                address: formatNominatimAddress(first.address, first.display_name ?? query) ?? query,
            });
        } catch {
            setAddressResolveError('Suche nach Ort oder Adresse ist aktuell nicht verfügbar.');
        } finally {
            setIsSearching(false);
        }
    };

    const parsedLatitude = useMemo(() => parseCoordinateInput(latitudeInput), [latitudeInput]);
    const parsedLongitude = useMemo(() => parseCoordinateInput(longitudeInput), [longitudeInput]);
    const hasCoordinateInput = latitudeInput.trim().length > 0 || longitudeInput.trim().length > 0;
    const coordinateValidationError = useMemo(() => {
        if (!hasCoordinateInput) {
            return undefined;
        }

        if (parsedLatitude == null || parsedLongitude == null) {
            return 'Bitte geben Sie Breitengrad und Längengrad an.';
        }

        if (Number.isNaN(parsedLatitude) || Number.isNaN(parsedLongitude)) {
            return 'Koordinaten müssen als Zahl angegeben werden.';
        }

        if (parsedLatitude < -90 || parsedLatitude > 90) {
            return 'Breitengrad muss zwischen -90 und 90 liegen.';
        }

        if (parsedLongitude < -180 || parsedLongitude > 180) {
            return 'Längengrad muss zwischen -180 und 180 liegen.';
        }

        return undefined;
    }, [hasCoordinateInput, parsedLatitude, parsedLongitude]);

    const canApplyCoordinates = !isMapDisabled &&
        coordinateValidationError == null &&
        parsedLatitude != null &&
        parsedLongitude != null &&
        !Number.isNaN(parsedLatitude) &&
        !Number.isNaN(parsedLongitude);

    const applyCoordinates = useCallback(() => {
        if (!canApplyCoordinates || parsedLatitude == null || parsedLongitude == null) {
            return;
        }

        updatePoint(parsedLatitude, parsedLongitude);
        void resolveAddressForPoint(parsedLatitude, parsedLongitude);
    }, [canApplyCoordinates, parsedLatitude, parsedLongitude, resolveAddressForPoint, updatePoint]);

    const mapAddressLine = useMemo(() => {
        if (props.value?.address != null && props.value.address.trim().length > 0) {
            return {
                text: props.value.address.trim(),
                isError: false,
            };
        }

        if (!hasCoordinates) {
            return undefined;
        }

        if (isResolvingAddress) {
            return {
                text: 'Adresse wird ermittelt ...',
                isError: false,
            };
        }

        if (addressResolveError != null) {
            return {
                text: addressResolveError,
                isError: true,
            };
        }

        return {
            text: 'Keine Adresse verfügbar',
            isError: false,
        };
    }, [addressResolveError, hasCoordinates, isResolvingAddress, props.value?.address]);

    const handleResetMapView = () => {
        mapRef.current?.setView(configuredCenter, zoom);
    };

    const handleClear = useCallback(() => {
        reverseLookupRequestId.current += 1;
        setIsResolvingAddress(false);
        setAddressResolveError(undefined);
        setSearchQuery('');
        setLatitudeInput('');
        setLongitudeInput('');
        props.onChange(null);

        mapRef.current?.setView(configuredCenter, zoom);
    }, [configuredCenter.lat, configuredCenter.lon, props.onChange, zoom]);

    const handleClearSearchInput = useCallback(() => {
        if (hasPointValue && props.required !== true) {
            handleClear();
            return;
        }

        setSearchQuery('');
        setAddressResolveError(undefined);
    }, [handleClear, hasPointValue, props.required]);

    const handleLatitudeInputChange = useCallback((nextLatitudeInput: string) => {
        setLatitudeInput(nextLatitudeInput);

        if (nextLatitudeInput.trim().length === 0 && longitudeInput.trim().length === 0 && hasPointValue) {
            handleClear();
        }
    }, [handleClear, hasPointValue, longitudeInput]);

    const handleLongitudeInputChange = useCallback((nextLongitudeInput: string) => {
        setLongitudeInput(nextLongitudeInput);

        if (latitudeInput.trim().length === 0 && nextLongitudeInput.trim().length === 0 && hasPointValue) {
            handleClear();
        }
    }, [handleClear, hasPointValue, latitudeInput]);

    const effectiveError = props.error ?? (
        inputMode === 'coordinates' ? coordinateValidationError : undefined
    ) ?? addressResolveError;

    return (
        <FormFieldGroup
            id={props.id}
            label={props.label}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={(groupContext) => {
                const suppliedLabelAction = typeof props.labelAction === 'function'
                    ? props.labelAction(groupContext)
                    : props.labelAction;

                return (
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{alignItems: 'center'}}
                    >
                        {suppliedLabelAction}

                        <ToggleButtonGroup
                            size="small"
                            value={inputMode}
                            exclusive
                            aria-label={`Eingabeweg für ${props.label}`}
                            sx={{
                                '& .MuiToggleButton-root': {
                                    minHeight: 28,
                                    px: 1,
                                    py: 0.25,
                                    fontSize: '0.75rem',
                                    lineHeight: 1.2,
                                    textTransform: 'none',
                                },
                            }}
                            onChange={(_, value: InputMode | null) => {
                                if (value != null && !props.disabled && !props.busy) {
                                    setInputMode(value);
                                }
                            }}
                        >
                            <ToggleButton
                                value="search"
                                disabled={props.disabled || props.busy}
                            >
                                Adresse/Ort
                            </ToggleButton>
                            <ToggleButton
                                value="coordinates"
                                disabled={props.disabled || props.busy}
                            >
                                Koordinaten
                            </ToggleButton>
                        </ToggleButtonGroup>
                    </Stack>
                );
            }}
            hint={props.hint}
            error={effectiveError}
            required={props.required}
            disabled={props.disabled}
            readOnly={props.readOnly}
            busy={props.busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(groupContext: FormFieldGroupContext) => (
                <Stack
                    spacing={1.5}
                    sx={props.controlSx}
                >
                    {inputMode === 'search' && (
                        <MapPointTextInput
                            label="Adresse oder Ort"
                            value={searchQuery}
                            onChange={setSearchQuery}
                            groupContext={groupContext}
                            groupError={effectiveError}
                            placeholder="Adresse oder Ort suchen"
                            inputMode="search"
                            disabled={props.disabled}
                            busy={props.busy}
                            loading={isSearching}
                            readOnly={props.readOnly}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter' && !isMapDisabled) {
                                    event.preventDefault();
                                    void handleSearch();
                                }
                            }}
                            endAdornment={(
                                <InputAdornment position="end">
                                    <Stack
                                        direction="row"
                                        spacing={0.25}
                                        sx={{alignItems: 'center'}}
                                    >
                                        {canClearSearchInput && (
                                            <IconButton
                                                size="small"
                                                onClick={handleClearSearchInput}
                                                disabled={isMapDisabled}
                                                aria-label={clearSearchInputLabel}
                                            >
                                                <ClearOutlinedIcon fontSize="small" />
                                            </IconButton>
                                        )}

                                        <IconButton
                                            size="small"
                                            onClick={() => {
                                                void handleSearch();
                                            }}
                                            disabled={isMapDisabled || isSearching || searchQuery.trim().length < 3}
                                            aria-label="Ort suchen"
                                            aria-busy={isSearching || undefined}
                                        >
                                            {isSearching
                                                ? <CircularProgress size={18} aria-hidden="true" />
                                                : <SearchOutlinedIcon />}
                                        </IconButton>
                                    </Stack>
                                </InputAdornment>
                            )}
                        />
                    )}

                    {inputMode === 'coordinates' && (
                        <Stack
                            direction={{xs: 'column', md: 'row'}}
                            spacing={1}
                        >
                            <MapPointTextInput
                                label="Breitengrad"
                                value={latitudeInput}
                                onChange={handleLatitudeInputChange}
                                groupContext={groupContext}
                                groupError={effectiveError}
                                inputMode="decimal"
                                disabled={props.disabled}
                                busy={props.busy}
                                readOnly={props.readOnly}
                                onKeyDown={(event) => {
                                    if (event.key === 'Enter' && !isMapDisabled) {
                                        event.preventDefault();
                                        applyCoordinates();
                                    }
                                }}
                            />
                            <MapPointTextInput
                                label="Längengrad"
                                value={longitudeInput}
                                onChange={handleLongitudeInputChange}
                                groupContext={groupContext}
                                groupError={effectiveError}
                                inputMode="decimal"
                                disabled={props.disabled}
                                busy={props.busy}
                                readOnly={props.readOnly}
                                onKeyDown={(event) => {
                                    if (event.key === 'Enter' && !isMapDisabled) {
                                        event.preventDefault();
                                        applyCoordinates();
                                    }
                                }}
                            />
                        </Stack>
                    )}

                    <Paper
                        data-map-point-surface
                        variant="outlined"
                        sx={{
                            overflow: 'hidden',
                            position: 'relative',
                        }}
                    >
                        <Stack
                            component="div"
                            spacing={0.75}
                            sx={{
                                position: 'absolute',
                                top: 8,
                                right: 8,
                                zIndex: 1000,
                            }}
                        >
                            <Tooltip
                                title="Ansicht zurücksetzen"
                                arrow
                            >
                                <span>
                                    <IconButton
                                        size="small"
                                        onClick={handleResetMapView}
                                        disabled={isMapDisabled}
                                        aria-label="Kartenansicht zurücksetzen"
                                        sx={{
                                            bgcolor: 'background.paper',
                                            border: '1px solid',
                                            borderColor: 'divider',
                                            boxShadow: '0 1px 5px rgba(0, 0, 0, 0.65)',
                                            '&:hover': {
                                                bgcolor: 'background.paper',
                                            },
                                        }}
                                    >
                                        <MyLocationOutlinedIcon fontSize="small" />
                                    </IconButton>
                                </span>
                            </Tooltip>
                            {
                                hasPointValue &&
                                <Tooltip
                                    title="Kartenpunkt leeren"
                                    arrow
                                >
                                    <span>
                                        <IconButton
                                            size="small"
                                            onClick={handleClear}
                                            disabled={isMapDisabled}
                                            aria-label="Kartenpunkt leeren"
                                            sx={{
                                                bgcolor: 'background.paper',
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                boxShadow: '0 1px 5px rgba(0, 0, 0, 0.65)',
                                                '&:hover': {
                                                    bgcolor: 'background.paper',
                                                },
                                            }}
                                        >
                                            <ClearOutlinedIcon fontSize="small" />
                                        </IconButton>
                                    </span>
                                </Tooltip>
                            }
                            {
                                inputMode === 'coordinates' &&
                                <Tooltip
                                    title="Koordinaten auf Karte anzeigen"
                                    arrow
                                >
                                    <span>
                                        <IconButton
                                            size="small"
                                            onClick={applyCoordinates}
                                            disabled={!canApplyCoordinates}
                                            aria-label="Koordinaten auf Karte anzeigen"
                                            sx={{
                                                bgcolor: 'background.paper',
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                boxShadow: '0 1px 5px rgba(0, 0, 0, 0.65)',
                                                '&:hover': {
                                                    bgcolor: 'background.paper',
                                                },
                                            }}
                                        >
                                            <NearMeOutlinedIcon fontSize="small" />
                                        </IconButton>
                                    </span>
                                </Tooltip>
                            }
                        </Stack>
                        <Stack
                            sx={{
                                height: 240,
                                position: 'relative',
                                userSelect: 'none',
                            }}
                        >
                            <LeafletPointPickerMap
                                ref={mapRef}
                                center={mapCenter}
                                zoom={zoom}
                                disabled={isMapDisabled}
                                marker={mapMarker}
                                onPick={handleMapPick}
                                ariaLabel={`Karte zur Auswahl von ${props.label}`}
                                ariaDescribedBy={groupContext.describedBy}
                                style={{
                                    width: '100%',
                                    height: '100%',
                                }}
                            />
                        </Stack>

                        {mapAddressLine != null && (
                            <Stack
                                direction="row"
                                spacing={1.25}
                                role={!mapAddressLine.isError ? 'status' : undefined}
                                aria-live={!mapAddressLine.isError ? 'polite' : undefined}
                                aria-atomic={!mapAddressLine.isError ? 'true' : undefined}
                                sx={{
                                    alignItems: 'center',
                                    minWidth: 0,
                                    px: 1.5,
                                    py: 1,
                                    borderTop: '1px solid',
                                    borderColor: 'divider',
                                    backgroundColor: 'action.hover',
                                }}
                            >
                                <Stack
                                    aria-hidden="true"
                                    sx={{
                                        width: 20,
                                        height: 20,
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        flexShrink: 0,
                                        color: mapAddressLine.isError ? 'error.main' : 'text.secondary',
                                    }}
                                >
                                    {isResolvingAddress
                                        ? <CircularProgress size={16} color="inherit" />
                                        : <LocationOnOutlinedIcon sx={{fontSize: 20}} />}
                                </Stack>

                                <Stack spacing={0.125} sx={{minWidth: 0}}>
                                    <Typography
                                        variant="caption"
                                        color={mapAddressLine.isError ? 'error.main' : 'text.secondary'}
                                        sx={{lineHeight: 1.2}}
                                    >
                                        Ermittelte Adresse
                                    </Typography>
                                    <Typography
                                        variant="body2"
                                        color={mapAddressLine.isError ? 'error.main' : 'text.primary'}
                                        title={mapAddressLine.text}
                                        sx={{
                                            whiteSpace: 'nowrap',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                        }}
                                    >
                                        {mapAddressLine.text}
                                    </Typography>
                                </Stack>
                            </Stack>
                        )}
                    </Paper>
                </Stack>
            )}
        </FormFieldGroup>
    );
}

import {
    CircularProgress,
    IconButton,
    InputAdornment,
    Paper,
    Stack,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography
} from '@mui/material';
import MyLocationOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MyLocation';
import NearMeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/NearMe';
import SearchOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Search';
import ClearOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {MapPointValue} from '../../models/elements/form/input/map-point-field-element';
import {
    LeafletPointPickerMap,
    type LeafletPoint,
    type LeafletPointPickerMapHandle
} from './leaflet-point-picker-map';

interface MapPointFieldComponentProps {
    label: string;
    value?: MapPointValue | null;
    onChange: (value: MapPointValue | null) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    zoom?: number;
    centerLatitude?: number;
    centerLongitude?: number;
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
    const isMapDisabled = props.disabled === true || props.busy === true;
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
                text: `Ermittelte Adresse: ${props.value.address.trim()}`,
                isError: false,
            };
        }

        if (!hasCoordinates) {
            return undefined;
        }

        if (isResolvingAddress) {
            return {
                text: 'Ermittelte Adresse wird geladen ...',
                isError: false,
            };
        }

        if (addressResolveError != null) {
            return {
                text: `Ermittelte Adresse: ${addressResolveError}`,
                isError: true,
            };
        }

        return {
            text: 'Ermittelte Adresse: Keine Adresse verfügbar',
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

    return (
        <Stack spacing={1.5}>
            <Stack
                direction="row"
                spacing={1}
                justifyContent="space-between"
                alignItems="center"
            >
                <Typography
                    sx={{
                        fontWeight: 'medium',
                    }}
                >
                    {props.label}{props.required ? ' *' : ''}
                </Typography>
                <ToggleButtonGroup
                    size="small"
                    value={inputMode}
                    exclusive
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
                        if (value != null) {
                            setInputMode(value);
                        }
                    }}
                >
                    <ToggleButton value="search">
                        Adresse/Ort
                    </ToggleButton>
                    <ToggleButton value="coordinates">
                        Koordinaten
                    </ToggleButton>
                </ToggleButtonGroup>
            </Stack>

            {
                inputMode === 'search' &&
                <TextField
                    label="Adresse oder Ort"
                    value={searchQuery}
                    onChange={(event) => {
                        setSearchQuery(event.target.value);
                    }}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                            event.preventDefault();
                            void handleSearch();
                        }
                    }}
                    placeholder="Adresse oder Ort suchen"
                    helperText={props.error ?? addressResolveError ?? props.hint}
                    error={props.error != null || addressResolveError != null}
                    disabled={props.disabled || props.busy}
                    slotProps={{
                        input: {
                            endAdornment: (
                                <InputAdornment position="end">
                                    <Stack
                                        direction="row"
                                        spacing={0.25}
                                        alignItems="center"
                                    >
                                        {
                                            canClearSearchInput &&
                                            <IconButton
                                                size="small"
                                                onClick={handleClearSearchInput}
                                                disabled={props.disabled || props.busy}
                                                aria-label={clearSearchInputLabel}
                                            >
                                                <ClearOutlinedIcon fontSize="small" />
                                            </IconButton>
                                        }

                                        <IconButton
                                            size="small"
                                            onClick={() => {
                                                void handleSearch();
                                            }}
                                            disabled={props.disabled || props.busy || isSearching || searchQuery.trim().length < 3}
                                            aria-label="Ort suchen"
                                        >
                                            {isSearching ? <CircularProgress size={18} /> : <SearchOutlinedIcon />}
                                        </IconButton>
                                    </Stack>
                                </InputAdornment>
                            ),
                        },
                    }}
                />
            }

            {
                inputMode === 'coordinates' &&
                <Stack spacing={1}>
                    <Stack
                        direction={{
                            xs: 'column',
                            md: 'row',
                        }}
                        spacing={1}
                    >
                        <TextField
                            label="Breitengrad"
                            value={latitudeInput}
                            onChange={(event) => {
                                handleLatitudeInputChange(event.target.value);
                            }}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter') {
                                    event.preventDefault();
                                    applyCoordinates();
                                }
                            }}
                            error={coordinateValidationError != null || props.error != null}
                            disabled={isMapDisabled}
                            fullWidth
                        />
                        <TextField
                            label="Längengrad"
                            value={longitudeInput}
                            onChange={(event) => {
                                handleLongitudeInputChange(event.target.value);
                            }}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter') {
                                    event.preventDefault();
                                    applyCoordinates();
                                }
                            }}
                            error={coordinateValidationError != null || props.error != null}
                            disabled={isMapDisabled}
                            fullWidth
                        />
                    </Stack>
                    {
                        (coordinateValidationError != null || props.error != null || addressResolveError != null || props.hint != null) &&
                        <Typography
                            variant="caption"
                            color={(coordinateValidationError != null || props.error != null || addressResolveError != null) ? 'error.main' : 'text.secondary'}
                        >
                            {coordinateValidationError ?? props.error ?? addressResolveError ?? props.hint}
                        </Typography>
                    }
                </Stack>
            }

            <Paper
                variant="outlined"
                sx={{
                    height: 240,
                    overflow: 'hidden',
                    position: 'relative',
                    userSelect: 'none',
                }}
            >
                <Stack
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
                <LeafletPointPickerMap
                    ref={mapRef}
                    center={mapCenter}
                    zoom={zoom}
                    disabled={isMapDisabled}
                    marker={mapMarker}
                    onPick={handleMapPick}
                    style={{
                        width: '100%',
                        height: '100%',
                    }}
                />
            </Paper>

            {
                mapAddressLine != null &&
                <Stack
                    direction="row"
                    spacing={1}
                    alignItems="center"
                >
                    <Stack
                        sx={{
                            width: 14,
                            height: 14,
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                        }}
                    >
                        {isResolvingAddress && <CircularProgress size={14} />}
                    </Stack>
                    <Typography
                        variant="body2"
                        color={mapAddressLine.isError ? 'error.main' : 'text.secondary'}
                        sx={{
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                        }}
                    >
                        {mapAddressLine.text}
                    </Typography>
                </Stack>
            }
        </Stack>
    );
}

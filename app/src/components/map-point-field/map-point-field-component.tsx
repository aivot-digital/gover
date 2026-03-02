import {Box, CircularProgress, IconButton, InputAdornment, Paper, Stack, TextField, Typography} from '@mui/material';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import {useEffect, useMemo, useRef, useState} from 'react';
import {MapPointValue} from '../../models/elements/form/input/map-point-field-element';
import {AttributionControl, MapContainer, Marker, TileLayer, useMap, useMapEvents} from 'react-leaflet';
import type {LeafletEventHandlerFnMap} from 'leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface MapPointFieldComponentProps {
    label: string;
    value?: MapPointValue | null;
    onChange: (value: MapPointValue | undefined) => void;
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

function clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value));
}

function normalizeZoom(zoom?: number): number {
    if (zoom == null || Number.isNaN(zoom)) {
        return 14;
    }

    return clamp(Math.round(zoom), 1, 19);
}

function normalizePoint(value: MapPointValue): MapPointValue | undefined {
    const latitude = value.latitude ?? undefined;
    const longitude = value.longitude ?? undefined;
    const address = value.address?.trim().length ? value.address.trim() : undefined;

    if (latitude == null && longitude == null && address == null) {
        return undefined;
    }

    return {
        latitude,
        longitude,
        address,
    };
}

const markerIcon = L.divIcon({
    className: '',
    html: '<div style="width:18px;height:18px;border-radius:50%;background:#1A73E8;border:2px solid #fff;box-shadow:0 0 0 1px rgba(0,0,0,0.25);"></div>',
    iconSize: [18, 18],
    iconAnchor: [9, 9],
});

function MapClickHandler(props: {
    disabled: boolean;
    onPick: (lat: number, lon: number) => void;
}) {
    useMapEvents({
        click: (event) => {
            if (props.disabled) {
                return;
            }

            props.onPick(event.latlng.lat, event.latlng.lng);
        },
    });

    return null;
}

export function MapPointFieldComponent(props: MapPointFieldComponentProps) {
    const zoom = normalizeZoom(props.zoom);
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const mapRef = useRef<L.Map | null>(null);
    const [latitudeInput, setLatitudeInput] = useState('');
    const [longitudeInput, setLongitudeInput] = useState('');

    const hasCoordinates = props.value?.latitude != null && props.value?.longitude != null;
    const markerLat = props.value?.latitude ?? undefined;
    const markerLon = props.value?.longitude ?? undefined;
    const isMapDisabled = props.disabled === true || props.busy === true;

    const mapCenter = useMemo(() => {
        if (hasCoordinates && markerLat != null && markerLon != null) {
            return {
                lat: markerLat,
                lon: markerLon,
            };
        }

        if (props.centerLatitude != null && props.centerLongitude != null) {
            return {
                lat: props.centerLatitude,
                lon: props.centerLongitude,
            };
        }

        return DEFAULT_CENTER;
    }, [hasCoordinates, markerLat, markerLon, props.centerLatitude, props.centerLongitude]);

    useEffect(() => {
        setLatitudeInput(props.value?.latitude != null ? props.value.latitude.toFixed(6) : '');
    }, [props.value?.latitude]);

    useEffect(() => {
        setLongitudeInput(props.value?.longitude != null ? props.value.longitude.toFixed(6) : '');
    }, [props.value?.longitude]);

    const updatePoint = (latitude: number, longitude: number, address?: string) => {
        props.onChange(normalizePoint({
            latitude,
            longitude,
            address: address ?? props.value?.address,
        }));

        if (mapRef.current != null) {
            mapRef.current.panTo([latitude, longitude], {
                animate: true,
            });
        }
    };

    const handleSearch = async () => {
        const query = searchQuery.trim();
        if (query.length < 3) {
            return;
        }

        try {
            setIsSearching(true);
            const response = await window.fetch(
                `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(query)}`,
            );

            if (!response.ok) {
                return;
            }

            const entries: any[] = await response.json();
            const first = entries?.[0];
            if (first == null) {
                return;
            }

            const latitude = Number.parseFloat(first.lat);
            const longitude = Number.parseFloat(first.lon);
            if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
                return;
            }

            updatePoint(latitude, longitude, first.display_name ?? query);
        } finally {
            setIsSearching(false);
        }
    };

    const markerEvents: LeafletEventHandlerFnMap = useMemo(() => ({
        dragend: (event) => {
            const marker = event.target as L.Marker;
            const latlng = marker.getLatLng();
            updatePoint(latlng.lat, latlng.lng);
        },
    }), [props.value?.address]);

    return (
        <Stack spacing={1.5}>
            <TextField
                label={`${props.label}${props.required ? ' *' : ''}`}
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
                helperText={props.error ?? props.hint}
                error={props.error != null}
                disabled={props.disabled || props.busy}
                slotProps={{
                    input: {
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton
                                    onClick={() => {
                                        void handleSearch();
                                    }}
                                    disabled={props.disabled || props.busy || isSearching || searchQuery.trim().length < 3}
                                    aria-label="Ort suchen"
                                >
                                    {isSearching ? <CircularProgress size={18} /> : <SearchOutlinedIcon />}
                                </IconButton>
                            </InputAdornment>
                        ),
                    },
                }}
            />

            <Paper
                variant="outlined"
                sx={{
                    height: 240,
                    overflow: 'hidden',
                    position: 'relative',
                    userSelect: 'none',
                }}
            >
                <MapContainer
                    center={[mapCenter.lat, mapCenter.lon]}
                    zoom={zoom}
                    style={{
                        width: '100%',
                        height: '100%',
                    }}
                    dragging={!isMapDisabled}
                    doubleClickZoom={!isMapDisabled}
                    scrollWheelZoom={!isMapDisabled}
                    touchZoom={!isMapDisabled}
                    boxZoom={!isMapDisabled}
                    keyboard={!isMapDisabled}
                    zoomControl={!isMapDisabled}
                    attributionControl={false}
                    ref={(mapInstance) => {
                        mapRef.current = mapInstance;
                    }}
                >
                    <TileLayer
                        attribution='&copy; OpenStreetMap-Mitwirkende'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    <AttributionControl
                        position="bottomright"
                        prefix={false}
                    />

                    <MapClickHandler
                        disabled={isMapDisabled}
                        onPick={(lat, lon) => {
                            updatePoint(lat, lon);
                        }}
                    />

                    {
                        hasCoordinates && markerLat != null && markerLon != null &&
                        <Marker
                            position={[markerLat, markerLon]}
                            draggable={!isMapDisabled}
                            icon={markerIcon}
                            eventHandlers={markerEvents}
                        />
                    }
                </MapContainer>
            </Paper>

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
                        setLatitudeInput(event.target.value);
                    }}
                    onBlur={() => {
                        const lat = Number.parseFloat(latitudeInput.replace(',', '.'));
                        const lon = Number.parseFloat(longitudeInput.replace(',', '.'));

                        if (Number.isNaN(lat) || Number.isNaN(lon)) {
                            return;
                        }

                        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                            return;
                        }

                        updatePoint(lat, lon);
                    }}
                    disabled={isMapDisabled}
                    fullWidth
                />
                <TextField
                    label="Längengrad"
                    value={longitudeInput}
                    onChange={(event) => {
                        setLongitudeInput(event.target.value);
                    }}
                    onBlur={() => {
                        const lat = Number.parseFloat(latitudeInput.replace(',', '.'));
                        const lon = Number.parseFloat(longitudeInput.replace(',', '.'));

                        if (Number.isNaN(lat) || Number.isNaN(lon)) {
                            return;
                        }

                        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                            return;
                        }

                        updatePoint(lat, lon);
                    }}
                    disabled={isMapDisabled}
                    fullWidth
                />
            </Stack>

            {
                props.value?.address != null &&
                <Typography variant="body2">
                    {props.value.address}
                </Typography>
            }
        </Stack>
    );
}

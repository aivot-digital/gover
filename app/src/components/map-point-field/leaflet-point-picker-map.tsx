import {CSSProperties, forwardRef, useCallback, useEffect, useImperativeHandle, useRef} from 'react';
import L from 'leaflet';

export interface LeafletPoint {
    lat: number;
    lon: number;
}

export interface LeafletPointPickerMapHandle {
    panTo: (point: LeafletPoint) => void;
    setView: (point: LeafletPoint, zoom: number) => void;
}

interface LeafletPointPickerMapProps {
    center: LeafletPoint;
    zoom: number;
    disabled: boolean;
    marker?: LeafletPoint | null;
    onPick: (point: LeafletPoint) => void;
    ariaLabel: string;
    ariaDescribedBy?: string;
    style?: CSSProperties;
}

const markerIcon = L.divIcon({
    className: '',
    html: '<div style="width:18px;height:18px;border-radius:50%;background:#1A73E8;border:2px solid #fff;box-shadow:0 0 0 1px rgba(0,0,0,0.25);"></div>',
    iconSize: [18, 18],
    iconAnchor: [9, 9],
});

export const LeafletPointPickerMap = forwardRef<LeafletPointPickerMapHandle, LeafletPointPickerMapProps>(function LeafletPointPickerMap(props, ref) {
    const mapContainerRef = useRef<HTMLDivElement | null>(null);
    const mapRef = useRef<L.Map | null>(null);
    const markerRef = useRef<L.Marker | null>(null);
    const zoomControlRef = useRef<L.Control.Zoom | null>(null);
    const lastAppliedMapCenterRef = useRef<string | null>(null);
    const resizeFrameRef = useRef<number | null>(null);
    const markerLat = props.marker?.lat;
    const markerLon = props.marker?.lon;

    const scheduleInvalidateSize = useCallback(() => {
        if (resizeFrameRef.current != null) {
            window.cancelAnimationFrame(resizeFrameRef.current);
        }

        resizeFrameRef.current = window.requestAnimationFrame(() => {
            resizeFrameRef.current = null;
            mapRef.current?.invalidateSize({
                debounceMoveend: true,
                pan: false,
            });
        });
    }, []);

    useImperativeHandle(ref, () => ({
        panTo: (point) => {
            mapRef.current?.panTo([point.lat, point.lon], {
                animate: true,
            });
        },
        setView: (point, zoom) => {
            lastAppliedMapCenterRef.current = `${point.lat}:${point.lon}:${zoom}`;
            mapRef.current?.setView([point.lat, point.lon], zoom, {
                animate: true,
            });
        },
    }), []);

    useEffect(() => {
        const container = mapContainerRef.current;
        if (container == null || mapRef.current != null) {
            return;
        }

        const map = L.map(container, {
            center: [props.center.lat, props.center.lon],
            zoom: props.zoom,
            dragging: !props.disabled,
            doubleClickZoom: !props.disabled,
            scrollWheelZoom: !props.disabled,
            touchZoom: !props.disabled,
            boxZoom: !props.disabled,
            keyboard: !props.disabled,
            zoomControl: false,
            attributionControl: false,
        });

        mapRef.current = map;
        lastAppliedMapCenterRef.current = `${props.center.lat}:${props.center.lon}:${props.zoom}`;

        L.control.attribution({
            position: 'bottomright',
            prefix: false,
        }).addTo(map);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap-Mitwirkende</a>',
        }).addTo(map);

        scheduleInvalidateSize();

        return () => {
            if (resizeFrameRef.current != null) {
                window.cancelAnimationFrame(resizeFrameRef.current);
                resizeFrameRef.current = null;
            }

            map.remove();
            mapRef.current = null;
            markerRef.current = null;
            zoomControlRef.current = null;
            lastAppliedMapCenterRef.current = null;
        };
    }, [scheduleInvalidateSize]);

    useEffect(() => {
        const container = mapContainerRef.current;
        if (container == null || mapRef.current == null) {
            return;
        }

        scheduleInvalidateSize();

        if (typeof ResizeObserver === 'undefined') {
            window.addEventListener('resize', scheduleInvalidateSize);

            return () => {
                window.removeEventListener('resize', scheduleInvalidateSize);
            };
        }

        const resizeObserver = new ResizeObserver(() => {
            scheduleInvalidateSize();
        });

        resizeObserver.observe(container);

        return () => {
            resizeObserver.disconnect();
        };
    }, [scheduleInvalidateSize]);

    useEffect(() => {
        const map = mapRef.current;
        const container = mapContainerRef.current;
        if (map == null || container == null) {
            return;
        }

        const handlers = [
            map.dragging,
            map.doubleClickZoom,
            map.scrollWheelZoom,
            map.touchZoom,
            map.boxZoom,
            map.keyboard,
        ];

        handlers.forEach((handler) => {
            if (props.disabled) {
                handler.disable();
            } else {
                handler.enable();
            }
        });

        container.tabIndex = props.disabled ? -1 : 0;

        if (props.disabled) {
            zoomControlRef.current?.remove();
            zoomControlRef.current = null;
        } else if (zoomControlRef.current == null) {
            zoomControlRef.current = L.control.zoom().addTo(map);
        }
    }, [props.disabled]);

    useEffect(() => {
        const map = mapRef.current;
        if (map == null) {
            return;
        }

        const centerKey = `${props.center.lat}:${props.center.lon}:${props.zoom}`;
        if (lastAppliedMapCenterRef.current === centerKey) {
            return;
        }

        lastAppliedMapCenterRef.current = centerKey;
        map.setView([props.center.lat, props.center.lon], props.zoom, {
            animate: true,
        });
    }, [props.center.lat, props.center.lon, props.zoom]);

    useEffect(() => {
        const map = mapRef.current;
        if (map == null) {
            return;
        }

        const handleClick = (event: L.LeafletMouseEvent) => {
            if (props.disabled) {
                return;
            }

            props.onPick({
                lat: event.latlng.lat,
                lon: event.latlng.lng,
            });
        };

        map.on('click', handleClick);

        return () => {
            map.off('click', handleClick);
        };
    }, [props.disabled, props.onPick]);

    useEffect(() => {
        const map = mapRef.current;
        if (map == null) {
            return;
        }

        if (markerLat == null || markerLon == null) {
            markerRef.current?.remove();
            markerRef.current = null;
            return;
        }

        const marker = markerRef.current ?? L.marker([markerLat, markerLon], {
            draggable: !props.disabled,
            icon: markerIcon,
        }).addTo(map);

        markerRef.current = marker;
        marker.setLatLng([markerLat, markerLon]);
        marker.setIcon(markerIcon);

        if (props.disabled) {
            marker.dragging?.disable();
        } else {
            marker.dragging?.enable();
        }

        const handleDragEnd = () => {
            const latlng = marker.getLatLng();
            props.onPick({
                lat: latlng.lat,
                lon: latlng.lng,
            });
        };

        marker.on('dragend', handleDragEnd);

        return () => {
            marker.off('dragend', handleDragEnd);
        };
    }, [markerLat, markerLon, props.disabled, props.onPick]);

    return (
        <div
            ref={mapContainerRef}
            role="region"
            aria-label={props.ariaLabel}
            aria-describedby={props.ariaDescribedBy}
            aria-disabled={props.disabled || undefined}
            style={props.style}
        />
    );
});

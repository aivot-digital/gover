import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {MapPointFieldComponent} from './map-point-field-component';

vi.mock('./leaflet-point-picker-map', async () => {
    const React = await import('react');

    return {
        LeafletPointPickerMap: React.forwardRef((_props: unknown, ref) => {
            React.useImperativeHandle(ref, () => ({
                panTo: () => undefined,
                setView: () => undefined,
            }));

            return <div data-testid="point-picker-map" />;
        }),
    };
});

const selectedPoint = {
    latitude: 52.52,
    longitude: 13.405,
    address: 'Berlin',
};

describe('MapPointFieldComponent', () => {
    it('should clear an optional selected point via the search input clear action', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        render(
            <MapPointFieldComponent
                label="Kartenpunkt"
                value={selectedPoint}
                onChange={onChange}
            />,
        );

        await user.click(screen.getByRole('button', {name: 'Kartenpunkt-Auswahl löschen'}));

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('should clear the selected point when both coordinate inputs are emptied', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        render(
            <MapPointFieldComponent
                label="Kartenpunkt"
                value={selectedPoint}
                onChange={onChange}
            />,
        );

        await user.click(screen.getByRole('button', {name: 'Koordinaten'}));
        await user.clear(screen.getByLabelText('Breitengrad'));

        expect(onChange).not.toHaveBeenCalled();

        await user.clear(screen.getByLabelText('Längengrad'));

        expect(onChange).toHaveBeenCalledWith(null);
    });
});

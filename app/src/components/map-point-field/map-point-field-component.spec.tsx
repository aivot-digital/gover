import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {MapPointFieldComponent} from './map-point-field-component';

vi.mock('./leaflet-point-picker-map', async () => {
    const React = await import('react');

    return {
        LeafletPointPickerMap: React.forwardRef((props: {
            ariaLabel: string;
            ariaDescribedBy?: string;
            disabled: boolean;
        }, ref) => {
            React.useImperativeHandle(ref, () => ({
                panTo: () => undefined,
                setView: () => undefined,
            }));

            return (
                <div
                    data-testid="point-picker-map"
                    role="region"
                    aria-label={props.ariaLabel}
                    aria-describedby={props.ariaDescribedBy}
                    aria-disabled={props.disabled || undefined}
                />
            );
        }),
    };
});

const selectedPoint = {
    latitude: 52.52,
    longitude: 13.405,
    address: 'Berlin',
};

describe('MapPointFieldComponent', () => {
    it('presents the point as one field group with labelled input paths and map', () => {
        render(
            <MapPointFieldComponent
                label="Kartenpunkt"
                value={selectedPoint}
                onChange={vi.fn()}
                hint="Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte."
            />,
        );

        const fieldGroup = screen.getByRole('group', {name: 'Kartenpunkt – optional'});
        expect(fieldGroup).toHaveAccessibleDescription(
            'Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte.',
        );

        const searchInput = screen.getByRole('textbox', {name: 'Adresse oder Ort'});
        expect(searchInput).toHaveAccessibleDescription(
            'Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte.',
        );
        expect(screen.getByRole('group', {name: 'Eingabeweg für Kartenpunkt'})).toBeInTheDocument();

        const map = screen.getByRole('region', {name: 'Karte zur Auswahl von Kartenpunkt'});
        expect(map).toHaveAccessibleDescription(
            'Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte.',
        );
        expect(screen.getByRole('button', {name: 'Kartenansicht zurücksetzen'})).toBeInTheDocument();
        const addressStatus = screen.getByRole('status');
        expect(addressStatus).toHaveTextContent('Ermittelte Adresse');
        expect(addressStatus).toHaveTextContent('Berlin');
        expect(addressStatus.closest('[data-map-point-surface]')).toContainElement(map);
    });

    it('renders the input path switch beside the label and preserves supplied label actions', () => {
        render(
            <MapPointFieldComponent
                label="Kartenpunkt"
                value={selectedPoint}
                onChange={vi.fn()}
                labelAction={<button type="button">Weitere Feldaktion</button>}
            />,
        );

        const inputPathSwitch = screen.getByRole('group', {name: 'Eingabeweg für Kartenpunkt'});
        const suppliedLabelAction = screen.getByRole('button', {name: 'Weitere Feldaktion'});
        const labelActionSlot = inputPathSwitch.closest('[data-form-field-label-action]');

        expect(labelActionSlot).not.toBeNull();
        expect(labelActionSlot).toContainElement(suppliedLabelAction);
    });

    it('renders externally labelled coordinate inputs without optional suffixes', async () => {
        const user = userEvent.setup();

        render(
            <MapPointFieldComponent
                label="Kartenpunkt"
                value={selectedPoint}
                onChange={vi.fn()}
            />,
        );

        await user.click(screen.getByRole('button', {name: 'Koordinaten'}));

        expect(screen.getByRole('textbox', {name: 'Breitengrad'})).toHaveValue('52.520000');
        expect(screen.getByRole('textbox', {name: 'Längengrad'})).toHaveValue('13.405000');
        expect(screen.getByRole('button', {name: 'Koordinaten auf Karte anzeigen'})).toBeInTheDocument();
    });

    it('associates one group error with the active input and map', () => {
        render(
            <MapPointFieldComponent
                label="Kartenpunkt"
                value={null}
                onChange={vi.fn()}
                error="Wählen Sie einen Kartenpunkt."
                required
            />,
        );

        const fieldGroup = screen.getByRole('group', {name: 'Kartenpunkt'});
        const searchInput = screen.getByRole('textbox', {name: 'Adresse oder Ort'});
        const map = screen.getByRole('region', {name: 'Karte zur Auswahl von Kartenpunkt'});

        expect(fieldGroup).toHaveAttribute('aria-invalid', 'true');
        expect(searchInput).toHaveAttribute('aria-invalid', 'true');
        expect(searchInput).toHaveAccessibleDescription('Wählen Sie einen Kartenpunkt.');
        expect(map).toHaveAccessibleDescription('Wählen Sie einen Kartenpunkt.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

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

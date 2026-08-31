import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {AssetSelector} from './asset-selector';

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../dialogs/select-asset-dialog/select-asset-dialog', () => ({
    SelectAssetDialog: ({show}: {show: boolean}) => show
        ? <div role="dialog" aria-label="Dateiauswahl"/>
        : null,
}));

describe('AssetSelector', () => {
    it('exposes a dialog picker instead of a read-only text field', () => {
        render(
            <AssetSelector
                label="Datei"
                selectLabel="Datei auswählen"
                value={null}
                onChange={vi.fn()}
            />,
        );

        const picker = screen.getByRole('button', {name: 'Datei – optional Keine Datei ausgewählt'});
        expect(picker).toHaveAttribute('aria-haspopup', 'dialog');
        expect(picker).toHaveAttribute('aria-expanded', 'false');
        expect(getComputedStyle(picker.parentElement!).minHeight).toBe('44px');
        expect(getComputedStyle(picker.parentElement!).backgroundColor).toBe('rgba(0, 0, 0, 0)');
        expect(screen.getByText('Keine Datei ausgewählt')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datei: Auswahl entfernen'})).toBeDisabled();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();

        fireEvent.click(picker);

        expect(screen.getByRole('dialog', {name: 'Dateiauswahl'})).toBeInTheDocument();
        expect(picker).toHaveAttribute('aria-expanded', 'true');
        expect(picker).toHaveAttribute('aria-controls');
    });
});

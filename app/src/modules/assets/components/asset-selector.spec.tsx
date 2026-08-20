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
    it('keeps the native field accessible and opens the selection dialog', () => {
        render(
            <AssetSelector
                label="Datei"
                selectLabel="Datei auswählen"
                value={null}
                onChange={vi.fn()}
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Datei'});
        expect(input).toHaveAttribute('readonly');
        expect(screen.getByText('Keine Datei ausgewählt')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Auswahl entfernen'})).toBeDisabled();

        fireEvent.click(input);

        expect(screen.getByRole('dialog', {name: 'Dateiauswahl'})).toBeInTheDocument();
    });
});

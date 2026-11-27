import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {AssetSelector} from './asset-selector';

const {dispatch, retrieveByKey, retrieveStorageProvider, dialogSpy} = vi.hoisted(() => ({
    dispatch: vi.fn(), retrieveByKey: vi.fn(), retrieveStorageProvider: vi.fn(), dialogSpy: vi.fn(),
}));
vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => dispatch,
}));

vi.mock('../../../dialogs/select-asset-dialog/select-asset-dialog', () => ({
    SelectAssetDialog: (props: {show: boolean}) => {
        dialogSpy(props);
        return props.show
        ? <div role="dialog" aria-label="Dateiauswahl"/>
        : null;
    },
}));
vi.mock('../assets-api-service', () => ({
    AssetsApiService: class {
        retrieveByKey = retrieveByKey;
        retrieveStorageProvider = retrieveStorageProvider;
    },
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

    it('preserves an existing selection and marks a provider mismatch until it is permitted again', async () => {
        retrieveByKey.mockResolvedValue({storageProviderId: 3, filename: 'Vorlage.pdf', pathFromRoot: '/Vorlage.pdf'});
        retrieveStorageProvider.mockResolvedValue({name: 'Vorlagen'});
        const onChange = vi.fn();
        const props = {label: 'Datei', selectLabel: 'Datei auswählen', value: 'stored-key', error: '', onChange};
        const {rerender} = render(<AssetSelector {...props} storageProviderId={7}/>);
        expect(await screen.findByRole('alert')).toHaveTextContent('Die ausgewählte Datei gehört nicht zum erlaubten Speicheranbieter.');
        expect(screen.getByText('Vorlage.pdf')).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        expect(dialogSpy).toHaveBeenLastCalledWith(expect.objectContaining({storageProviderId: 7}));
        rerender(<AssetSelector {...props}/>);
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    });
});

import {fireEvent, render, screen, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ResourceInputGallery} from './resource-input-gallery';
import {AssetsApiService} from '../assets/assets-api-service';
import {SecretsApiService} from '../secrets/secrets-api-service';
import {ElementsApiService} from '../elements/elements-api-service';
import {AssetVisibility} from '../assets/models/asset-visibility';

const mocks = vi.hoisted(() => ({dispatch: vi.fn(), api: {}, assetDialog: vi.fn()}));

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../hooks/use-app-selector', () => ({useAppSelector: () => true}));
vi.mock('../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../dialogs/select-asset-dialog/select-asset-dialog', () => ({
    SelectAssetDialog: (props: {id: string; show: boolean; title: string; onSelect: (key: string) => void}) => {
        mocks.assetDialog(props);
        return props.show ? (
            <div id={props.id} role="dialog" aria-label={props.title}>
                <button onClick={() => props.onSelect('certificate-key')}>Zertifikat verwenden</button>
            </div>
        ) : null;
    },
}));
vi.mock('../secrets/dialogs/secret-select-dialog', () => ({
    SecretSelectDialog: (props: {id: string; open: boolean; onSelect: (secret: object) => void}) => props.open ? (
        <div id={props.id} role="dialog" aria-label="Geheimnis auswählen">
            <button onClick={() => props.onSelect({
                key: 'secret-key', name: 'Produktionszugang', description: 'Externer Dienst', value: 'never-show-this',
            })}>Geheimnis verwenden</button>
        </div>
    ) : null,
}));

describe('ResourceInputGallery through the view dispatcher', () => {
    beforeEach(() => {
        vi.spyOn(AssetsApiService.prototype, 'retrieveByKey').mockResolvedValue({
            filename: 'client.pem', storageProviderId: 1, pathFromRoot: '/certificates/client.pem',
        } as Awaited<ReturnType<AssetsApiService['retrieveByKey']>>);
        vi.spyOn(AssetsApiService.prototype, 'retrieveStorageProvider').mockResolvedValue({
            name: 'Dokumente',
        } as Awaited<ReturnType<AssetsApiService['retrieveStorageProvider']>>);
        vi.spyOn(SecretsApiService.prototype, 'retrieve').mockResolvedValue({
            key: 'secret-key', name: 'Produktionszugang', description: 'Externer Dienst', value: 'never-show-this',
        });
        vi.spyOn(ElementsApiService.prototype, 'derive');
    });

    it('renders the actual resource elements with normal field dimensions and semantics', () => {
        render(<ResourceInputGallery/>);
        const region = screen.getByRole('region', {name: 'Ressourcenreferenzen'});
        const certificate = within(region).getByRole('button', {
            name: 'Client-Zertifikat – optional Kein Zertifikat ausgewählt',
        });
        const secret = within(region).getByRole('button', {name: 'Zugangsschlüssel Geheimnis auswählen'});
        expect(certificate).toHaveAccessibleDescription('Private Datei für die Anmeldung beim externen Dienst.');
        expect(secret).toHaveAccessibleDescription('Der geheime Wert wird erst bei der Ausführung verwendet. Erforderliche Auswahl.');
        expect(secret).toHaveAttribute('aria-required', 'true');
        for (const control of [certificate, secret]) {
            expect(control).toHaveAttribute('aria-haspopup', 'dialog');
            expect(getComputedStyle(control.parentElement!).minHeight).toBe('44px');
            expect(getComputedStyle(control.parentElement!).backgroundColor).toBe('rgba(0, 0, 0, 0)');
        }
        expect(mocks.assetDialog).toHaveBeenCalledWith(expect.objectContaining({
            visibility: AssetVisibility.Private,
            mimetype: ['application/x-pem-file', 'application/pkix-cert'],
        }));
        expect(ElementsApiService.prototype.derive).not.toHaveBeenCalled();
    });

    it('preserves literal references through selection, read-only mode and clearing', async () => {
        render(<ResourceInputGallery/>);
        fireEvent.click(screen.getByRole('button', {name: 'Client-Zertifikat – optional Kein Zertifikat ausgewählt'}));
        fireEvent.click(screen.getByRole('button', {name: 'Zertifikat verwenden'}));
        const certificate = await screen.findByRole('button', {name: 'Client-Zertifikat – optional client.pem'});
        await screen.findByText('Dokumente: /certificates/client.pem');

        fireEvent.click(screen.getByRole('button', {name: 'Zugangsschlüssel Geheimnis auswählen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis verwenden'}));
        const secret = await screen.findByRole('button', {name: 'Zugangsschlüssel Produktionszugang'});
        expect(screen.queryByText('never-show-this')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('switch', {name: 'Schreibgeschützt'}));
        for (const control of [certificate, secret]) expect(control).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Client-Zertifikat: Auswahl entfernen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Zugangsschlüssel: Auswahl entfernen'})).toBeDisabled();

        fireEvent.click(screen.getByRole('switch', {name: 'Schreibgeschützt'}));
        fireEvent.click(screen.getByRole('button', {name: 'Client-Zertifikat: Auswahl entfernen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Zugangsschlüssel: Auswahl entfernen'}));
        expect(screen.getByRole('button', {name: 'Client-Zertifikat – optional Kein Zertifikat ausgewählt'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Zugangsschlüssel Geheimnis auswählen'})).toBeEnabled();
        expect(ElementsApiService.prototype.derive).not.toHaveBeenCalled();
    });
});

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AssetPickerDialog} from './asset-picker-dialog';
import {AssetVisibility} from '../../modules/assets/models/asset-visibility';
import {StorageProviderType} from '../../modules/storage/enums/storage-provider-type';
import {type StorageIndexItem} from '../../modules/storage/entities/storage-index-item-entity';

const {
    dispatchSpy,
    assetExplorerSpy,
    confirmSpy,
    listStorageProvidersSpy,
    retrieveAssetSpy,
    updateAssetSpy,
} = vi.hoisted(() => ({
    dispatchSpy: vi.fn(),
    assetExplorerSpy: vi.fn(),
    confirmSpy: vi.fn(),
    listStorageProvidersSpy: vi.fn(),
    retrieveAssetSpy: vi.fn(),
    updateAssetSpy: vi.fn(),
}));

vi.mock('../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatchSpy}));
vi.mock('../../providers/confirm-provider', () => {
    return {useConfirm: () => confirmSpy};
});
vi.mock('../../modules/permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));
vi.mock('../../modules/assets/assets-api-service', () => ({
    AssetsApiService: class {
        listStorageProviders = listStorageProvidersSpy;
        retrieveInStorageProvider = retrieveAssetSpy;
        updateInStorageProvider = updateAssetSpy;
    },
}));
vi.mock('../../modules/storage/components/asset-explorer', () => ({
    AssetExplorer: (props: unknown) => {
        assetExplorerSpy(props);
        return <div data-testid="asset-explorer"/>;
    },
}));

describe('AssetPickerDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        confirmSpy.mockResolvedValue(true);
        listStorageProvidersSpy.mockResolvedValue([
            {
                id: 7,
                name: 'Lokale Dokumente & Medien',
                readOnlyStorage: false,
                maxFileSizeInBytes: 10_000_000,
                metadataAttributes: [],
            },
        ]);
    });

    it('renders provider and selection criteria with the shared field layout', async () => {
        render(
            <AssetPickerDialog
                title="PDF-Vorlage auswählen"
                show
                mimeType="application/pdf"
                onSelectAsset={vi.fn()}
                onCancel={vi.fn()}
            />,
        );

        await screen.findByText('Lokale Dokumente & Medien');
        const providerLabel = screen.getByTitle('Speicheranbieter');
        const providerSelect = document.getElementById(providerLabel.getAttribute('for')!)!;
        const criteriaLabel = screen.getByTitle('Auswahlkriterien');
        const criteria = document.getElementById(criteriaLabel.getAttribute('for')!)!;

        expect(providerSelect).toHaveTextContent('Lokale Dokumente & Medien');
        expect(providerSelect).toHaveAttribute('role', 'combobox');
        expect(providerSelect).toHaveAttribute('aria-labelledby', providerLabel.id);
        expect(criteria).toHaveTextContent('Dateityp: PDF-Dokument');
        expect(criteria).toHaveAttribute('role', 'group');
        expect(criteria).toHaveAttribute('aria-labelledby', criteriaLabel.id);
        expect(getComputedStyle(criteria).minHeight).toBe('44px');
        expect(document.querySelector('.MuiInputLabel-root')).not.toBeInTheDocument();
        expect(screen.getByTestId('asset-explorer')).toBeInTheDocument();
    });

    it('limits credential selection to private assets', async () => {
        render(
            <AssetPickerDialog
                title="Privaten Schlüssel auswählen"
                show
                mimeType="application/x-pem-file"
                visibility={AssetVisibility.Private}
                onSelectAsset={vi.fn()}
                onCancel={vi.fn()}
            />,
        );

        await screen.findByText('Lokale Dokumente & Medien');

        expect(screen.getByText('Nur private Dateien')).toBeInTheDocument();
        expect(screen.getByText(/Dateityp:/)).toBeInTheDocument();
        expect(assetExplorerSpy).toHaveBeenLastCalledWith(expect.objectContaining({
            filterMimeTypes: ['application/x-pem-file'],
            filterVisibility: AssetVisibility.Private,
        }));
    });

    it('keeps private files available and publishes them for public selection', async () => {
        const onSelectAsset = vi.fn();
        const privateAsset = {
            key: 'asset-key',
            storageProviderId: 7,
            storagePathFromRoot: '/branding/logo.png',
            filename: 'logo.png',
            created: '2026-09-11T08:00:00Z',
            uploaderId: 'user-id',
            contentType: 'image/png',
            isPrivate: true,
            metadata: {},
        };
        const publishedAsset = {
            ...privateAsset,
            isPrivate: false,
        };
        retrieveAssetSpy.mockResolvedValue(privateAsset);
        updateAssetSpy.mockResolvedValue(publishedAsset);

        render(
            <AssetPickerDialog
                title="Logo auswählen"
                show
                mimeType="image"
                visibility={AssetVisibility.Public}
                onSelectAsset={onSelectAsset}
                onCancel={vi.fn()}
            />,
        );

        await screen.findByText('Lokale Dokumente & Medien');

        expect(screen.getByText('Öffentlicher Zugriff erforderlich')).toBeInTheDocument();
        const explorerProps = assetExplorerSpy.mock.calls.at(-1)?.[0] as {
            filterVisibility: AssetVisibility;
            onFileSelect: (item: StorageIndexItem) => Promise<void>;
        };
        expect(explorerProps.filterVisibility).toBe(AssetVisibility.All);

        await act(async () => explorerProps.onFileSelect({
            storageProviderId: 7,
            storageProviderType: StorageProviderType.Assets,
            pathFromRoot: privateAsset.storagePathFromRoot,
            directory: false,
            filename: privateAsset.filename,
            mimeType: privateAsset.contentType,
            sizeInBytes: 1024,
            missing: false,
            metadata: {},
            created: privateAsset.created,
            updated: privateAsset.created,
            assetKey: privateAsset.key,
            assetUploaderId: privateAsset.uploaderId,
            assetIsPrivate: true,
        }));

        expect(confirmSpy).toHaveBeenCalledWith(expect.objectContaining({
            title: 'Datei öffentlich schalten?',
            confirmButtonText: 'Öffentlich schalten und auswählen',
        }));
        expect(retrieveAssetSpy).toHaveBeenCalledWith(privateAsset.storagePathFromRoot, 7);
        expect(updateAssetSpy).toHaveBeenCalledWith(
            privateAsset.storagePathFromRoot,
            publishedAsset,
            7,
        );
        expect(onSelectAsset).toHaveBeenCalledWith(
            publishedAsset.key,
            publishedAsset.storagePathFromRoot,
            publishedAsset.storageProviderId,
        );
    });

    it('reloads providers and remounts the asset explorer whenever it is reopened', async () => {
        const props = {
            title: 'Zertifikat auswählen',
            mimeType: 'application/x-pkcs12',
            onSelectAsset: vi.fn(),
            onCancel: vi.fn(),
        };
        const {rerender} = render(<AssetPickerDialog {...props} show/>);

        await waitFor(() => expect(listStorageProvidersSpy).toHaveBeenCalledOnce());
        await screen.findByTestId('asset-explorer');
        const explorerRenderCount = assetExplorerSpy.mock.calls.length;

        rerender(<AssetPickerDialog {...props} show={false}/>);
        rerender(<AssetPickerDialog {...props} show/>);

        await waitFor(() => expect(listStorageProvidersSpy).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(assetExplorerSpy.mock.calls.length).toBeGreaterThan(explorerRenderCount));
    });

    it('fixes the provider and rejects selections from another provider', async () => {
        listStorageProvidersSpy.mockResolvedValue([
            {id: 3, name: 'Andere Dateien'}, {id: 7, name: 'Vorlagen'},
        ]);
        const onSelectAsset = vi.fn();
        render(<AssetPickerDialog title="Datei auswählen" show storageProviderId={7}
                                  onSelectAsset={onSelectAsset} onCancel={vi.fn()}/>);
        await screen.findByText('Vorlagen');
        const providerSelect = screen.getByRole('combobox', {name: /Speicheranbieter/});
        expect(providerSelect).toHaveTextContent('Vorlagen');
        expect(providerSelect).toHaveAttribute('aria-disabled', 'true');
        expect(assetExplorerSpy).toHaveBeenLastCalledWith(expect.objectContaining({providerId: 7}));
        const {onFileSelect} = assetExplorerSpy.mock.lastCall![0];
        const item = {storageProviderId: 3, assetKey: 'asset-key', pathFromRoot: '/file.pdf', directory: false};
        await act(() => onFileSelect(item));
        expect(onSelectAsset).not.toHaveBeenCalled();
        await act(() => onFileSelect({...item, storageProviderId: 7}));
        expect(onSelectAsset).toHaveBeenCalledWith('asset-key', '/file.pdf', 7);
    });

    it('keeps the provider selectable without a restriction', async () => {
        listStorageProvidersSpy.mockResolvedValue([{id: 3, name: 'Dateien'}, {id: 7, name: 'Vorlagen'}]);
        render(<AssetPickerDialog title="Datei auswählen" show
                                  onSelectAsset={vi.fn()} onCancel={vi.fn()}/>);
        await screen.findByText('Dateien');
        fireEvent.mouseDown(screen.getByRole('combobox', {name: /Speicheranbieter/}));
        fireEvent.click(await screen.findByRole('option', {name: 'Vorlagen'}));
        expect(assetExplorerSpy).toHaveBeenLastCalledWith(expect.objectContaining({providerId: 7}));
    });

    it('does not fall back to another provider when the configured provider is missing', async () => {
        render(<AssetPickerDialog title="Datei auswählen" show storageProviderId={99}
                                  onSelectAsset={vi.fn()} onCancel={vi.fn()}/>);
        expect(await screen.findByText('Der festgelegte Speicheranbieter ist nicht verfügbar.')).toBeVisible();
        expect(screen.queryByTestId('asset-explorer')).not.toBeInTheDocument();
    });

    it('updates the restriction while open and rejects an in-flight selection from the old provider', async () => {
        listStorageProvidersSpy.mockResolvedValue([{id: 7, name: 'Vorlagen'}, {id: 8, name: 'Logos'}]);
        let resolveAsset!: (asset: unknown) => void;
        retrieveAssetSpy.mockReturnValue(new Promise(resolve => { resolveAsset = resolve; }));
        const onSelectAsset = vi.fn();
        const props = {title: 'Datei auswählen', show: true, onSelectAsset, onCancel: vi.fn()};
        const {rerender} = render(<AssetPickerDialog {...props} storageProviderId={7}/>);
        await screen.findByText('Vorlagen');
        const {onFileSelect} = assetExplorerSpy.mock.lastCall![0];
        let pending!: Promise<void>;
        act(() => { pending = onFileSelect({storageProviderId: 7, pathFromRoot: '/file.pdf', directory: false}); });
        rerender(<AssetPickerDialog {...props} storageProviderId={8}/>);
        await screen.findByText('Logos');
        await act(async () => {
            resolveAsset({key: 'asset-key', storageProviderId: 7, storagePathFromRoot: '/file.pdf'});
            await pending;
        });
        expect(onSelectAsset).not.toHaveBeenCalled();
        expect(assetExplorerSpy).toHaveBeenLastCalledWith(expect.objectContaining({providerId: 8}));
    });

    it('does not report successful selection when the provider changes during publication', async () => {
        listStorageProvidersSpy.mockResolvedValue([{id: 7, name: 'Vorlagen'}, {id: 8, name: 'Logos'}]);
        const asset = {key: 'asset-key', storageProviderId: 7, storagePathFromRoot: '/file.pdf', isPrivate: true};
        retrieveAssetSpy.mockResolvedValue(asset);
        let resolvePublication!: (asset: unknown) => void;
        updateAssetSpy.mockReturnValue(new Promise(resolve => { resolvePublication = resolve; }));
        const onSelectAsset = vi.fn();
        const props = {title: 'Datei auswählen', show: true, visibility: AssetVisibility.Public, onSelectAsset, onCancel: vi.fn()};
        const {rerender} = render(<AssetPickerDialog {...props} storageProviderId={7}/>);
        await screen.findByText('Vorlagen');
        const {onFileSelect} = assetExplorerSpy.mock.lastCall![0];
        let pending!: Promise<void>;
        await act(async () => {
            pending = onFileSelect({storageProviderId: 7, pathFromRoot: '/file.pdf', directory: false});
        });
        expect(updateAssetSpy).toHaveBeenCalledOnce();
        rerender(<AssetPickerDialog {...props} storageProviderId={8}/>);
        await screen.findByText('Logos');
        await act(async () => {
            resolvePublication({...asset, isPrivate: false});
            await pending;
        });
        expect(onSelectAsset).not.toHaveBeenCalled();
        expect(dispatchSpy).not.toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Die Datei wurde veröffentlicht und ausgewählt.'}),
        }));
    });
});

import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {AssetPickerDialog} from './asset-picker-dialog';

vi.mock('../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../hooks/use-app-dispatch', () => {
    const dispatch = vi.fn();
    return {useAppDispatch: () => dispatch};
});
vi.mock('../../providers/confirm-provider', () => {
    const confirm = vi.fn();
    return {useConfirm: () => confirm};
});
vi.mock('../../modules/permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));
vi.mock('../../modules/assets/assets-api-service', () => ({
    AssetsApiService: class {
        listStorageProviders = vi.fn().mockResolvedValue([
            {
                id: 7,
                name: 'Lokale Dokumente & Medien',
                readOnlyStorage: false,
                maxFileSizeInBytes: 10_000_000,
                metadataAttributes: [],
            },
        ]);
    },
}));
vi.mock('../../modules/storage/components/asset-explorer', () => ({
    AssetExplorer: () => <div data-testid="asset-explorer"/>,
}));

describe('AssetPickerDialog', () => {
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
});

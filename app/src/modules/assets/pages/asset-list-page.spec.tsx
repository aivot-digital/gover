import {type ComponentProps, type ReactNode} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {GenericListPage} from '../../../components/generic-list-page/generic-list-page';
import {ConfirmProvider} from '../../../providers/confirm-provider';
import {type VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {AssetsApiService} from '../assets-api-service';
import {AssetListPage} from './asset-list-page';

const mocks = vi.hoisted(() => ({dispatch: vi.fn(), refresh: vi.fn()}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => ({})}));
vi.mock('../../../providers/prompt-provider', () => ({usePrompt: () => vi.fn()}));
vi.mock('../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: ReactNode}) => children,
}));
vi.mock('../../../components/generic-list-page/generic-list-page', () => ({
    GenericListPage: (props: ComponentProps<typeof GenericListPage<VStorageIndexItemWithAssetEntity>>) => {
        if (props.controlRef) props.controlRef.current = {refresh: mocks.refresh};
        const item = {directory: true, filename: 'Unterlagen', storageProviderId: 9, pathFromRoot: '/Unterlagen/'} as VStorageIndexItemWithAssetEntity;
        const actions = props.rowActions?.(item, {
            canCreate: true,
            canRead: () => true,
            canUpdate: () => true,
            hasPermission: () => true,
            getMissingPermissionTooltip: () => 'Keine Berechtigung',
        }) ?? [];
        return actions.map((action, index) => action !== 'separator' && 'onClick' in action ? (
            <button key={index} onClick={action.onClick}>{action.tooltip}</button>
        ) : null);
    },
}));

function renderPage() {
    render(<MemoryRouter><ConfirmProvider><AssetListPage/></ConfirmProvider></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', {name: 'Ordner löschen'}));
}

async function confirmDeletion() {
    fireEvent.change(screen.getByRole('textbox', {name: /Eingabe zur Bestätigung/}), {target: {value: 'Unterlagen'}});
    const confirm = screen.getByRole('button', {name: 'Löschen'});
    await waitFor(() => expect(confirm).toBeEnabled());
    fireEvent.click(confirm);
}

describe('Folder deletion', () => {
    it('cancels without deleting, reporting success, or refreshing the list', async () => {
        const remove = vi.spyOn(AssetsApiService.prototype, 'deleteFolder').mockResolvedValue(undefined);
        renderPage();
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(remove).not.toHaveBeenCalled();
        expect(mocks.dispatch).not.toHaveBeenCalled();
        expect(mocks.refresh).not.toHaveBeenCalled();
    });

    it('reports success and refreshes only after the deletion succeeds', async () => {
        let finish!: () => void;
        const remove = vi.spyOn(AssetsApiService.prototype, 'deleteFolder').mockReturnValue(
            new Promise<void>(resolve => {finish = resolve;}),
        );
        renderPage();
        await confirmDeletion();
        await waitFor(() => expect(remove).toHaveBeenCalledExactlyOnceWith(9, '/Unterlagen/'));
        expect(mocks.dispatch).not.toHaveBeenCalled();
        expect(mocks.refresh).not.toHaveBeenCalled();
        await act(async () => finish());
        expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Ordner erfolgreich gelöscht.'}),
        }));
        expect(mocks.refresh).toHaveBeenCalledTimes(1);
    });

    it('reports a failed deletion without refreshing the list', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.spyOn(AssetsApiService.prototype, 'deleteFolder').mockRejectedValue(new Error('Failed'));
        renderPage();
        await confirmDeletion();
        await waitFor(() => expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Der Ordner konnte nicht gelöscht werden.', severity: 'error'}),
        })));
        expect(mocks.refresh).not.toHaveBeenCalled();
    });
});

import React from 'react';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {
    StoragePathSelectorMode,
    type StoragePathSelectorInputElementValue,
} from '../../models/elements/form/input/storage-path-selector-input-element';
import {StoragePathSelectorInputComponent} from './storage-path-selector-input-component';
import {type StorageIndexItem} from '../../modules/storage/entities/storage-index-item-entity';

interface StorageExplorerTestProps {
    initialPath?: string | null;
    onFolderSelect?: (path: string) => void;
    onFileSelect?: (item: StorageIndexItem) => void;
    disableFileDialog?: boolean;
}

const storageExplorerState = vi.hoisted(() => ({
    props: undefined as StorageExplorerTestProps | undefined,
}));

vi.mock('../../modules/storage/components/storage-explorer', () => ({
    StorageExplorer: (props: StorageExplorerTestProps) => {
        storageExplorerState.props = props;
        return <div data-testid="storage-explorer"/>;
    },
}));

describe('StoragePathSelectorInputComponent', () => {
    beforeEach(() => {
        storageExplorerState.props = undefined;
    });

    it('should retain folder selection behavior by default', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        renderSelector(undefined, '/exports', onChange);

        await user.click(screen.getByRole('button', {name: 'Ordner auswählen'}));
        await screen.findByTestId('storage-explorer');

        expect(storageExplorerState.props?.initialPath).toBe('/exports/');
        expect(storageExplorerState.props?.onFileSelect).toBeUndefined();
        expect(storageExplorerState.props?.disableFileDialog).toBe(true);

        act(() => {
            storageExplorerState.props?.onFolderSelect?.('/archive');
        });

        expect(onChange).toHaveBeenCalledWith({
            storageProviderId: 77,
            path: '/archive/',
        });
        await waitFor(() => expect(screen.queryByTestId('storage-explorer')).not.toBeInTheDocument());
    });

    it('should select files and open the explorer in the selected file parent folder', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        renderSelector(StoragePathSelectorMode.File, '/certificates/client.pem', onChange);

        await user.click(screen.getByRole('button', {name: 'Datei auswählen'}));
        await screen.findByTestId('storage-explorer');

        expect(storageExplorerState.props?.initialPath).toBe('/certificates/');
        expect(storageExplorerState.props?.onFolderSelect).toBeUndefined();
        expect(storageExplorerState.props?.disableFileDialog).toBe(false);

        act(() => {
            storageExplorerState.props?.onFileSelect?.({
                pathFromRoot: '/certificates/server.pem',
            } as StorageIndexItem);
        });

        expect(onChange).toHaveBeenCalledWith({
            storageProviderId: 77,
            path: '/certificates/server.pem',
        });
        await waitFor(() => expect(screen.queryByTestId('storage-explorer')).not.toBeInTheDocument());
    });

    it('should open file mode at the root for templated paths', async () => {
        const user = userEvent.setup();

        renderSelector(StoragePathSelectorMode.File, '/certificates/{{ fileName }}', vi.fn());

        await user.click(screen.getByRole('button', {name: 'Datei auswählen'}));
        await screen.findByTestId('storage-explorer');

        expect(storageExplorerState.props?.initialPath).toBe('/');
    });
});

function renderSelector(
    mode: StoragePathSelectorMode | undefined,
    path: string | null,
    onChange: (value: StoragePathSelectorInputElementValue | null) => void,
) {
    return render(
        <StoragePathSelectorInputComponent
            label="Speicherpfad"
            mode={mode}
            value={{
                storageProviderId: 77,
                path,
            }}
            onChange={onChange}
            allowedStorageProviderTypes={[]}
        />,
    );
}

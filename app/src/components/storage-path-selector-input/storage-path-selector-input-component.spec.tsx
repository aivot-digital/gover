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
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../../theming/base-theme';

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

vi.mock('../../modules/storage/storage-providers-api-service', () => ({
    StorageProvidersApiService: class {
        listAll() {
            return Promise.resolve({content: []});
        }
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

        await user.click(screen.getByRole('button', {name: 'Speicherpfad: Ordner auswählen'}));
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

        await user.click(screen.getByRole('button', {name: 'Speicherpfad: Datei auswählen'}));
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

        await user.click(screen.getByRole('button', {name: 'Speicherpfad: Datei auswählen'}));
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

describe('StoragePathSelectorInputComponent accessibility', () => {
    it('exposes the composite value as a labelled group with labelled subfields', async () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <StoragePathSelectorInputComponent
                    label="Ablageort"
                    value={null}
                    onChange={vi.fn()}
                    storageProviderSelectHint="Wählen Sie einen Speicheranbieter."
                    hint="Die Unterlagen werden an diesem Ort abgelegt."
                />
            </ThemeProvider>,
        );

        const group = screen.getByTitle('Ablageort').closest('fieldset');
        expect(group).toHaveAccessibleName('Ablageort – optional');
        expect(group).toHaveAccessibleDescription('Die Unterlagen werden an diesem Ort abgelegt.');
        const providerInput = screen.getByLabelText('Speicheranbieter');
        const pathInput = screen.getByLabelText('Pfad');
        expect(providerInput).toHaveAccessibleDescription(/Wählen Sie einen Speicheranbieter/);
        expect(pathInput).toBeDisabled();
        expect(providerInput.closest('.MuiTextField-root')).not.toHaveClass('MuiFormControl-marginNormal');
        expect(pathInput.closest('.MuiTextField-root')).not.toHaveClass('MuiFormControl-marginNormal');
        expect(getComputedStyle(providerInput.closest('.MuiInputBase-root')!).minHeight).toBe('44px');
        expect(getComputedStyle(pathInput.closest('.MuiInputBase-root')!).minHeight).toBe('44px');

        await waitFor(() => expect(group).not.toHaveAttribute('aria-busy', 'true'));
    });
});

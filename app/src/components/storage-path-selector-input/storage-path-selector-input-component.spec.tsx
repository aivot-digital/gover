import React from 'react';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {type StoragePathSelectorInputElementValue} from '../../models/elements/form/input/storage-path-selector-input-element';
import {StoragePathSelectorInputComponent} from './storage-path-selector-input-component';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../../theming/base-theme';

interface StorageExplorerTestProps {
    initialPath?: string | null;
    onFolderSelect?: (path: string) => void;
    onFileSelect?: never;
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

        renderSelector('/exports', onChange);

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

    it('should open templated paths at the root', async () => {
        const user = userEvent.setup();

        renderSelector('/exports/{{ folderName }}', vi.fn());

        await user.click(screen.getByRole('button', {name: 'Speicherpfad: Ordner auswählen'}));
        await screen.findByTestId('storage-explorer');

        expect(storageExplorerState.props?.initialPath).toBe('/');
    });
});

function renderSelector(
    path: string | null,
    onChange: (value: StoragePathSelectorInputElementValue | null) => void,
) {
    return render(
        <StoragePathSelectorInputComponent
            label="Speicherpfad"
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

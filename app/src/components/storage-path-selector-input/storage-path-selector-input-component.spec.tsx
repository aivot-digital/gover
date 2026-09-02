import {render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {StoragePathSelectorInputComponent} from './storage-path-selector-input-component';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../../theming/base-theme';

vi.mock('../../modules/storage/storage-providers-api-service', () => ({
    StorageProvidersApiService: class {
        listAll() {
            return Promise.resolve({content: []});
        }
    },
}));

describe('StoragePathSelectorInputComponent', () => {
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

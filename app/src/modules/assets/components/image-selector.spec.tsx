import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ImageSelector} from './image-selector';

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));

vi.mock('../../../dialogs/select-asset-dialog/select-asset-dialog', () => ({
    SelectAssetDialog: ({id, show}: {id?: string; show: boolean}) => show
        ? <div id={id} role="dialog" aria-label="Bildauswahl"/>
        : null,
}));

describe('ImageSelector', () => {
    it('exposes its preview as a labelled dialog picker', () => {
        render(
            <ImageSelector
                label="Logo"
                hint="Wählen Sie ein öffentliches Bild aus."
                selectLabel="Logo auswählen"
                size={{aspectRatio: 2}}
                value={null}
                onChange={vi.fn()}
            />,
        );

        const picker = screen.getByRole('button', {name: 'Logo – optional Kein Bild ausgewählt'});
        expect(picker).toHaveAccessibleDescription('Wählen Sie ein öffentliches Bild aus.');
        expect(picker).toHaveAttribute('aria-haspopup', 'dialog');
        expect(screen.getByRole('button', {name: 'Logo: Auswahl entfernen'})).toBeDisabled();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();

        fireEvent.click(picker);

        expect(screen.getByRole('dialog', {name: 'Bildauswahl'})).toBeInTheDocument();
        expect(picker).toHaveAttribute('aria-expanded', 'true');
    });

    it('announces a required selection without adding a visible marker', () => {
        render(
            <ImageSelector
                label="Logo"
                hint="Wählen Sie ein Bild aus."
                selectLabel="Logo auswählen"
                size={{width: 64, height: 64}}
                value={null}
                onChange={vi.fn()}
                required
            />,
        );

        const picker = screen.getByRole('button', {name: 'Logo Kein Bild ausgewählt'});
        expect(picker).toHaveAccessibleDescription('Wählen Sie ein Bild aus. Erforderliche Auswahl.');
        expect(screen.queryByText(/optional/)).not.toBeInTheDocument();
    });
});

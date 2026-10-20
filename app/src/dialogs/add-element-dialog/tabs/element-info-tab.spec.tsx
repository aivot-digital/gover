import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementInfoTab} from './element-info-tab';
import {ElementType} from '../../../data/element-type/element-type';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

describe('ElementInfoTab multiple text input', () => {
    it('describes the field and provides working examples for suggestions, counts and pasting', () => {
        const onAddElement = vi.fn();
        render(<ElementInfoTab type={ElementType.ChipInput} onAddElement={onAddElement}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);
        expect(screen.getByText('Mehrfach-Texteingabe', {exact: true})).toBeVisible();
        expect(screen.getByText('Erfasst mehrere kurze Texte in einem Feld, jeweils als eigenen Eintrag.')).toBeVisible();
        const topics = screen.getByRole('combobox', {name: 'Themen'});
        expect(topics).toHaveAccessibleDescription(/2 von 5 Einträgen verwendet/);
        fireEvent.change(topics, {target: {value: 'Bild'}});
        fireEvent.click(screen.getByRole('option', {name: 'Bildung'}));
        expect(topics).toHaveAccessibleDescription(/3 von 5 Einträgen verwendet/);

        const references = screen.getByRole('combobox', {name: /^Aktenzeichen/});
        fireEvent.paste(references, {clipboardData: {getData: () => 'AZ-2026-001\nAZ-2026-002'}});
        expect(screen.getByText('AZ-2026-001')).toBeVisible();
        expect(screen.getByText('AZ-2026-002')).toBeVisible();

        fireEvent.click(screen.getByRole('button', {name: 'Hinzufügen'}));
        expect(onAddElement).toHaveBeenCalledWith(expect.objectContaining({type: ElementType.ChipInput, label: 'Mehrfach-Texteingabe'}));
    });
});

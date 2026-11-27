import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {BaseCodeTab} from './base-code-tab';
import {BaseCodeTabProps} from './base-code-tab-props';

vi.mock('../rich-text-input-component/rich-text-input-component', () => ({RichTextInputComponent: () => <div/>}));

function setup(overrides: Partial<BaseCodeTabProps> = {}) {
    const onSelectFunction = vi.fn();
    const onDeleteFunction = vi.fn();
    render(<BaseCodeTab label="Sichtbarkeit"
                        hasFunction
                        editable
                        allowsNoCode
                        allowsExpression
                        onRequirementsChange={vi.fn()}
                        onSelectFunction={onSelectFunction}
                        onDeleteFunction={onDeleteFunction} {...overrides}/>);
    return {
        onSelectFunction,
        onDeleteFunction,
    };
}

describe('function actions', () => {
    it('places deletion last, separated from changing the function type, and keeps confirmation', async () => {
        const {onDeleteFunction} = setup();
        fireEvent.click(screen.getByRole('button', {name: 'Mehr'}));
        const menu = screen.getByRole('menu');
        const items = within(menu).getAllByRole('menuitem');
        expect(items.map(item => item.textContent)).toEqual(['Anderen Funktionstyp auswählen', 'Funktion löschen']);
        const separator = within(menu).getByRole('separator');
        expect(separator.previousElementSibling).toBe(items[0]);
        expect(separator.nextElementSibling).toBe(items[1]);
        fireEvent.click(items[1]);
        const confirmation = await screen.findByRole('dialog', {name: 'Funktion löschen'});
        expect(onDeleteFunction).not.toHaveBeenCalled();
        fireEvent.click(within(confirmation).getByRole('button', {name: 'Abbrechen'}));
        expect(onDeleteFunction).not.toHaveBeenCalled();
    });

    it('uses the dialog title without a second section heading and confirms a type change', async () => {
        const {onSelectFunction} = setup();
        fireEvent.click(screen.getByRole('button', {name: 'Mehr'}));
        fireEvent.click(screen.getByRole('menuitem', {name: 'Anderen Funktionstyp auswählen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Funktionstyp auswählen'});
        expect(within(dialog).getByRole('heading', {
            name: 'Funktionstyp auswählen',
            level: 2,
        })).toBeInTheDocument();
        expect(within(dialog).queryByRole('heading', {name: 'No-/Low-Code-Funktion'})).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Funktion anlegen'})).not.toBeInTheDocument();
        const option = within(dialog).getByRole('region', {name: 'Low-Code – JavaScript'});
        fireEvent.click(within(option).getByRole('button', {name: 'Typ auswählen'}));
        expect(onSelectFunction).not.toHaveBeenCalled();
        expect(await screen.findByRole('dialog', {name: 'Funktionstyp ändern'})).toBeInTheDocument();
    });

    it('keeps the standalone creation heading and actions', () => {
        const {onSelectFunction} = setup({hasFunction: false});
        expect(screen.getByRole('heading', {name: 'No-/Low-Code-Funktion'})).toBeInTheDocument();
        const option = screen.getByRole('region', {name: 'No-Code – Regeln und Berechnungen'});
        fireEvent.click(within(option).getByRole('button', {name: 'Funktion anlegen'}));
        expect(onSelectFunction).toHaveBeenCalledWith('expression');
    });
});

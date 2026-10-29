import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import type {LinkButtonElement} from '../models/elements/form/content/link-button-element';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {LinkButtonEditor} from './link-button-editor';

const eventActions = [
    {key: 'staffTaskEvent', label: 'Aufgabenaktion für Mitarbeitende'},
    {key: 'customerTaskEvent', label: 'Aufgabenaktion für externe Beteiligte'},
] as const;

function renderEditor(overrides: Partial<LinkButtonElement> = {}, editable = true) {
    let element = {...generateElementWithDefaultValues(ElementType.LinkButton), ...overrides};
    const onPatch = vi.fn((patch: Partial<LinkButtonElement>) => {
        element = {...element, ...patch};
        view.rerender(editor());
    });
    const editor = () => <LinkButtonEditor element={element} onPatch={onPatch} editable={editable}
        hasSummaryLayoutParent={false} scope="application"/>;
    const view = render(editor());
    return {...view, onPatch, getElement: () => element};
}

async function selectAction(label: string) {
    fireEvent.mouseDown(screen.getByRole('combobox', {name: /^Aktion/}));
    fireEvent.click(await screen.findByRole('option', {name: new RegExp(`^${label}`)}));
}

describe('LinkButtonEditor action selection', () => {
    it.each(eventActions)('keeps $key selected while entering, clearing and reloading an event', async ({key, label}) => {
        const view = renderEditor({href: 'https://example.org'});
        await selectAction(label);
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveTextContent(label);
        expect(screen.queryByRole('textbox', {name: /^Link/})).not.toBeInTheDocument();
        expect(view.getElement().href).toBeNull();
        const input = screen.getByRole('textbox', {name: /^Ereigniskennung/});
        expect(input).toHaveValue('');
        expect(input).toHaveAccessibleDescription(/Eine frei gewählte Kennung legt keine neue Aktion an/);

        fireEvent.change(input, {target: {value: 'complete'}});
        fireEvent.blur(input);
        expect(view.getElement()[key]).toBe('complete');
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.blur(input);
        expect(view.getElement()[key]).toBe('');
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveTextContent(label);

        const saved = JSON.parse(JSON.stringify(view.getElement()));
        view.unmount();
        renderEditor(saved);
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveTextContent(label);
        expect(screen.getByRole('textbox', {name: /^Ereigniskennung/})).toHaveValue('');
    });

    it('switches between both event actions and back to a link without retaining incompatible targets', async () => {
        const view = renderEditor({staffTaskEvent: 'complete'});
        await selectAction('Aufgabenaktion für externe Beteiligte');
        expect(view.getElement()).toMatchObject({href: null, staffTaskEvent: null, customerTaskEvent: ''});
        await selectAction('Aufgabenaktion für Mitarbeitende');
        expect(view.getElement()).toMatchObject({href: null, staffTaskEvent: '', customerTaskEvent: null});
        await selectAction('Link öffnen');
        expect(view.getElement()).toMatchObject({staffTaskEvent: null, customerTaskEvent: null});
        expect(screen.queryByRole('textbox', {name: /^Ereigniskennung/})).not.toBeInTheDocument();
        const input = screen.getByRole('textbox', {name: /^Link/});
        fireEvent.change(input, {target: {value: 'https://example.org'}});
        fireEvent.blur(input);
        expect(view.getElement().href).toBe('https://example.org');
    });

    it.each(eventActions)('recognizes existing $key configurations without patching them', ({key, label}) => {
        const view = renderEditor({[key]: 'existing-action'});
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveTextContent(label);
        expect(screen.getByRole('textbox', {name: /^Ereigniskennung/})).toHaveValue('existing-action');
        expect(view.onPatch).not.toHaveBeenCalled();
    });

    it('shows a stored link before legacy event targets, matching runtime precedence', () => {
        const view = renderEditor({href: 'https://example.org', staffTaskEvent: 'complete'});
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveTextContent('Link öffnen');
        expect(screen.getByRole('textbox', {name: /^Link/})).toHaveValue('https://example.org');
        expect(view.onPatch).not.toHaveBeenCalled();
    });

    it('prefers a configured event over an empty legacy event field', () => {
        renderEditor({staffTaskEvent: '', customerTaskEvent: 'submit'});
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveTextContent('Aufgabenaktion für externe Beteiligte');
    });

    it('disables action configuration when editing is not allowed', () => {
        renderEditor({staffTaskEvent: 'complete'}, false);
        expect(screen.getByRole('combobox', {name: /^Aktion/})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('textbox', {name: /^Ereigniskennung/})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Symbol auswählen'})).toBeDisabled();
    });

    it('explains all actions in the open menu before an action is selected', async () => {
        renderEditor();
        fireEvent.mouseDown(screen.getByRole('combobox', {name: /^Aktion/}));
        expect(await screen.findByRole('option', {name: /^Link öffnen/})).toHaveTextContent('zum Beispiel eine Website oder einen Download');
        for (const {label} of eventActions) {
            const option = screen.getByRole('option', {name: new RegExp(`^${label}`)});
            expect(option).toHaveTextContent('Außerhalb dieser Ansicht ist die Schaltfläche deaktiviert');
            expect(option).toHaveTextContent('Vor dem Ausführen werden die Formulareingaben geprüft');
        }
    });

    it('selects, replaces and removes a leading icon using the shared picker', async () => {
        const view = renderEditor();
        fireEvent.click(screen.getByRole('button', {name: 'Symbol auswählen'}));
        let dialog = await screen.findByRole('dialog', {name: 'Symbol (Icon) für Schaltfläche auswählen'});
        fireEvent.click(within(dialog).getByRole('button', {name: 'Bestätigung (Haken)'}));
        expect(view.getElement().icon).toBe('check');
        expect(await screen.findByRole('group', {name: /^Symbol/})).toHaveTextContent('Bestätigung (Haken)');

        fireEvent.click(screen.getByRole('button', {name: 'Symbol ändern'}));
        dialog = await screen.findByRole('dialog', {name: 'Symbol (Icon) für Schaltfläche auswählen'});
        expect(within(dialog).getByRole('button', {name: 'Bestätigung (Haken)'})).toHaveAttribute('aria-pressed', 'true');
        fireEvent.click(within(dialog).getByRole('button', {name: 'Information (i-Symbol)'}));
        expect(view.getElement().icon).toBe('info');
        fireEvent.click(await screen.findByRole('button', {name: 'Symbol entfernen'}));
        expect(view.getElement().icon).toBeNull();
        expect(screen.getByRole('group', {name: /^Symbol/})).toHaveTextContent('Kein Symbol ausgewählt');
    });

    it('allows removing unavailable icons and keeps existing icon controls disabled without edit access', () => {
        const view = renderEditor({icon: 'unknown-icon'});
        expect(screen.getByText('Symbol nicht verfügbar')).toBeVisible();
        fireEvent.click(screen.getByRole('button', {name: 'Symbol entfernen'}));
        expect(view.getElement().icon).toBeNull();
        view.unmount();
        renderEditor({icon: 'check'}, false);
        expect(screen.getByRole('button', {name: 'Symbol ändern'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Symbol entfernen'})).toBeDisabled();
    });

    it.each([
        {label: 'Erfolg', value: 'success'}, {label: 'Information', value: 'info'},
        {label: 'Warnung', value: 'warning'}, {label: 'Fehler', value: 'error'},
    ])('stores the signal color $value', async ({label, value}) => {
        const view = renderEditor();
        fireEvent.mouseDown(screen.getByRole('combobox', {name: /^Farbe/}));
        fireEvent.click(await screen.findByRole('option', {name: label}));
        expect(view.getElement().color).toBe(value);
        expect(screen.getByRole('combobox', {name: /^Farbe/})).toHaveTextContent(label);
    });
});

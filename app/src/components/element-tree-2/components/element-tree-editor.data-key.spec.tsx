import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementTreeEditor} from './element-tree-editor';
import {ElementTreeContextProvider} from '../element-tree-context';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementsApiService} from '../../../modules/elements/elements-api-service';
import {type AnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {DefaultTabs} from '../../element-editor/default-tabs';

const confirm = vi.hoisted(() => vi.fn().mockResolvedValue(true));
vi.mock('../../../providers/confirm-provider', () => ({useConfirm: () => confirm}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../editors', () => ({editors: {}}));

function renderEditor({isNew = false, destinationKey, siblingKey}: {isNew?: boolean; destinationKey?: string; siblingKey?: string} = {}) {
    const initial = {...generateElementWithDefaultValues(ElementType.Text), id: 'field', label: 'Vorname', destinationKey};
    const form = {...(generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement), id: 'form'};
    const onApply = vi.fn();
    function Harness() {
        const [element, setElement] = useState(initial);
        const [newIds, setNewIds] = useState(new Set(isNew ? ['field'] : []));
        const [open, setOpen] = useState(true);
        return <ElementTreeContextProvider value={{
            root: form, editable: true, displayContext: ElementDisplayContext.CustomerFacing,
            allowElementIdEditing: false, allElements: [
                {element, parents: [form]},
                ...(siblingKey ? [{element: {...initial, id: 'sibling', destinationKey: siblingKey}, parents: [form]}] : []),
            ],
            newElementIds: newIds, markElementApplied: () => setNewIds(new Set()),
            scrollToElement: () => {}, canDropElement: () => false, moveElement: () => {},
            expandCommand: {type: null, version: 0}, initiallyExpandedSectionIds: new Set(),
        }}>
            <button onClick={() => setOpen(true)}>Erneut öffnen</button>
            <ElementTreeEditor value={element} parents={[form]} open={open} onClone={() => {}} onDelete={() => {}}
                onCancel={() => setOpen(false)} onChange={(updated) => {
                    onApply(updated);
                    setElement(updated);
                    setOpen(false);
                }}/>
        </ElementTreeContextProvider>;
    }
    render(<Harness/>);
    return {onApply};
}

describe('form data key editing session', () => {
    beforeEach(() => {
        window.history.replaceState({}, '', `/#field/${DefaultTabs.properties}`);
        vi.spyOn(ElementsApiService.prototype, 'recalculateReferencedIds').mockImplementation(async (element) => element);
    });

    it('generates a free key for a new field, follows its title, then freezes it after applying', async () => {
        const user = userEvent.setup();
        const {onApply} = renderEditor({isNew: true, siblingKey: 'vorname'});
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('vorname_2');
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Größe des Haushalts'}});
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('groesse_des_haushalts');
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
        expect((onApply.mock.calls[0][0] as AnyInputElement).destinationKey).toBe('groesse_des_haushalts');
        await waitFor(() => expect(screen.queryByRole('textbox', {name: /^Titel/})).not.toBeInTheDocument());
        await user.click(screen.getByRole('button', {name: 'Erneut öffnen'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('groesse_des_haushalts');
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveAttribute('readonly');
    });

    it('does not populate a pre-existing empty key when the title changes', () => {
        renderEditor();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('');
    });

    it('manually relocks without saving or resuming title-based generation', async () => {
        const user = userEvent.setup();
        const {onApply} = renderEditor({isNew: true});
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        fireEvent.change(input, {target: {value: 'eigener_schluessel'}});
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel sperren'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
        expect(input).toHaveAttribute('readonly');
        expect(input).toHaveValue('eigener_schluessel');
        expect(onApply).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
        expect((onApply.mock.calls[0][0] as AnyInputElement).destinationKey).toBe('eigener_schluessel');
    });

    it('keeps a deliberately cleared key empty after another title edit', async () => {
        renderEditor({isNew: true});
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        expect(confirm).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Vorgangsdatenpfad leeren'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('');
    });

    it('keeps an unlocked key editable across tabs and blocks applying an invalid path without blur', async () => {
        const {onApply} = renderEditor({destinationKey: 'person.name'});
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        expect(confirm).toHaveBeenCalledOnce();
        act(() => {
            window.history.replaceState({}, '', `/#field/${DefaultTabs.metadata}`);
            window.dispatchEvent(new HashChangeEvent('hashchange'));
        });
        act(() => {
            window.history.replaceState({}, '', `/#field/${DefaultTabs.properties}`);
            window.dispatchEvent(new HashChangeEvent('hashchange'));
        });
        const input = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        expect(input).not.toHaveAttribute('readonly');
        fireEvent.change(input, {target: {value: 'person.'}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(onApply).not.toHaveBeenCalled();
        expect(ElementsApiService.prototype.recalculateReferencedIds).not.toHaveBeenCalled();
    });
});

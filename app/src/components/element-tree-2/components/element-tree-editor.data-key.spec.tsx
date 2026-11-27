import {NoCodeOperand} from '../../../models/functions/no-code-expression';
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
import {type ElementPanelDraft} from '../element-panel-draft';
import {type EditorProps} from '@monaco-editor/react';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => api}));
const confirm = vi.hoisted(() => vi.fn().mockResolvedValue(true));
vi.mock('../../../providers/confirm-provider', () => ({useConfirm: () => confirm}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../editors', () => ({editors: {}}));
const monaco = vi.hoisted(() => ({props: {} as EditorProps}));
vi.mock('../../code-editor/monaco-editor', () => ({MonacoEditor: (props: EditorProps) => {
    monaco.props = props;
    return null;
}}));

function renderEditor({isNew = false, destinationKey, siblingKey, trackDraft = false, initialDraft, noCode}: {
    isNew?: boolean; destinationKey?: string; siblingKey?: string; trackDraft?: boolean; initialDraft?: ElementPanelDraft; noCode?: NoCodeOperand | null;
} = {}) {
    const initial = {...generateElementWithDefaultValues(ElementType.Text), id: 'field', label: 'Vorname', destinationKey};
    if (noCode !== undefined) initial.visibility = {
        type: 'NoCode', noCode, requirements: null, conditionSet: null, javascriptCode: null, referencedIds: [],
    };
    const form = {...(generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement), id: 'form'};
    const onApply = vi.fn();
    const onDraft = vi.fn<(draft: ElementPanelDraft | null) => void>();
    function Harness() {
        const [element, setElement] = useState(initial);
        const [newIds, setNewIds] = useState(new Set(isNew ? ['field'] : []));
        const [open, setOpen] = useState(true);
        const [panelDraft, setPanelDraft] = useState<ElementPanelDraft | null>(initialDraft ?? null);
        return <ElementTreeContextProvider value={{
            root: form,
            editable: true,
            displayContext: ElementDisplayContext.CustomerFacing,
            allowElementIdEditing: false,
            allElements: [
                {
                    element,
                    parents: [form],
                },
                ...(siblingKey ? [{
                    element: {
                        ...initial,
                        id: 'sibling',
                        destinationKey: siblingKey,
                    },
                    parents: [form],
                }] : []),
            ],
            newElementIds: newIds, markElementApplied: () => setNewIds(new Set()),
            panelDraft, onPanelDraftChange: trackDraft ? (draft) => {
                onDraft(draft);
                setPanelDraft(draft);
            } : undefined,
            scrollToElement: () => {}, canDropElement: () => false, moveElement: () => {},
            expandCommand: {type: null, version: 0}, initiallyExpandedSectionIds: new Set(),
        }}>
            <button onClick={() => setOpen(true)}>Erneut öffnen</button>
            <ElementTreeEditor value={element}
                               parents={[form]}
                               open={open}
                               onClone={() => {
                               }}
                               onDelete={() => {
                               }}
                               onCancel={() => setOpen(false)}
                               onChange={(updated) => {
                                   onApply(updated);
                                   setElement(updated);
                                   setOpen(false);
                               }}/>
        </ElementTreeContextProvider>;
    }
    const {unmount} = render(<Harness/>);
    return {onApply, onDraft, unmount};
}

describe('form data key editing session', () => {
    beforeEach(() => {
        api.get.mockReset().mockResolvedValue([]);
        window.history.replaceState({}, '', `/#field/${DefaultTabs.properties}`);
        vi.spyOn(ElementsApiService.prototype, 'recalculateReferencedIds').mockImplementation(async (element) => element);
    });

    it('applies an incomplete No-Code function, which the configuration check reports instead', async () => {
        const {onApply} = renderEditor({noCode: null});
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
        expect(onApply.mock.calls[0][0]).toMatchObject({
            label: 'Neuer Titel',
            visibility: {
                type: 'NoCode',
                noCode: null,
            },
        });
        expect(screen.queryByText('Vervollständigen Sie den No-Code-Ausdruck, bevor Sie das Element übernehmen.')).not.toBeInTheDocument();
    });

    it('applies an intentional empty No-Code value', async () => {
        const {onApply} = renderEditor({
            noCode: {
                type: 'NoCodeStaticValue',
                value: null,
            },
        });
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
    });

    it('generates a free key for a new field, follows its title, then freezes it after applying', async () => {
        const user = userEvent.setup();
        const {onApply} = renderEditor({
            isNew: true,
            siblingKey: 'vorname',
        });
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

    it('restores a manually edited invalid key without re-enabling title tracking', async () => {
        const first = renderEditor({isNew: true, trackDraft: true});
        expect(first.onDraft).not.toHaveBeenCalled();
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Datenschlüssel/}), {target: {value: 'person.'}});
        const saved = first.onDraft.mock.lastCall![0]!;
        first.unmount();

        const restored = renderEditor({trackDraft: true, initialDraft: saved});
        const key = screen.getByRole('textbox', {name: /^Datenschlüssel/});
        expect(key).not.toHaveAttribute('readonly');
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Nachname'}});
        expect(key).toHaveValue('person.');
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(key).toHaveAttribute('aria-invalid', 'true');
        expect(restored.onApply).not.toHaveBeenCalled();
        expect(restored.onDraft.mock.lastCall![0]).toMatchObject({mine: {destinationKey: 'person.'}, automaticDataKey: false});
    });

    it('keeps the restored key control unlocked after reverting all authored changes', async () => {
        const first = renderEditor({destinationKey: 'person', trackDraft: true});
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Rufname'}});
        const saved = first.onDraft.mock.lastCall![0]!;
        first.unmount();
        const restored = renderEditor({destinationKey: 'person', trackDraft: true, initialDraft: saved});
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Vorname'}});
        expect(restored.onDraft).toHaveBeenLastCalledWith(null);
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).not.toHaveAttribute('readonly');
    });

    it('preserves an external removal when reverting a title and discards it on Cancel', async () => {
        const field = {...generateElementWithDefaultValues(ElementType.Text), id: 'field', label: 'Vorname'};
        const pending = [{element: {...field, id: 'moved'}, parentId: 'other-group'}];
        const initialDraft = {
            base: field, mine: field, tab: DefaultTabs.properties, parents: [],
            automaticDataKey: false, dataKeyUnlocked: false, pendingMovedRemovals: pending,
        };
        const {onApply, onDraft} = renderEditor({trackDraft: true, initialDraft});
        const title = screen.getByRole('textbox', {name: /^Titel/});
        fireEvent.change(title, {target: {value: 'Rufname'}});
        fireEvent.change(title, {target: {value: 'Vorname'}});
        expect(onDraft.mock.lastCall![0]).toMatchObject({mine: field, pendingMovedRemovals: pending});
        await userEvent.setup().click(screen.getByRole('button', {name: 'Abbrechen'}));
        expect(confirm).toHaveBeenCalledOnce();
        expect(onDraft).toHaveBeenLastCalledWith(null);
        expect(onApply).not.toHaveBeenCalled();
    });

    it('records only an applied structure and retains it while reference recalculation is pending', async () => {
        const user = userEvent.setup();
        const {onDraft, onApply, unmount} = renderEditor({trackDraft: true});
        await user.click(screen.getByRole('tab', {name: 'Elementstruktur'}));
        const changed = {...JSON.parse(monaco.props.value!), label: 'Entwurf aus Struktur'};
        act(() => monaco.props.onMount?.({getValue: () => JSON.stringify(changed)} as never, {} as never));
        await user.click(screen.getByRole('switch', {name: 'Struktur manuell überschreiben'}));
        expect(onDraft).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Struktur anwenden'}));
        expect(onDraft.mock.lastCall![0]).toMatchObject({mine: changed, tab: DefaultTabs.structure});
        const calculation = Promise.withResolvers<typeof changed>();
        vi.mocked(ElementsApiService.prototype.recalculateReferencedIds).mockReturnValue(calculation.promise);
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));
        const snapshot = onDraft.mock.lastCall![0];
        expect(snapshot).toMatchObject({mine: changed});
        unmount();
        await act(async () => { calculation.resolve(changed); });
        expect(onDraft.mock.lastCall![0]).toBe(snapshot);
        expect(onApply).not.toHaveBeenCalled();
    });
});

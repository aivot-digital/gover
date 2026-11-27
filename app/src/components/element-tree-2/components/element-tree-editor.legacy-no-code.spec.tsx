import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementTreeEditor} from './element-tree-editor';
import {ElementTreeContextProvider} from '../element-tree-context';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementsApiService} from '../../../modules/elements/elements-api-service';
import {TextFieldElement} from '../../../models/elements/form/input/text-field-element';
import {FormLayoutElement} from '../../../models/elements/form-layout-element';
import {ConditionSetOperator} from '../../../data/condition-set-operator';
import {ConditionOperator} from '../../../data/condition-operator';
import {LegacyNoCodeSection} from '../../../modules/nocode/hooks/use-legacy-no-code-migration';
import {type ElementPanelDraft} from '../element-panel-draft';
import {NoCodeDataType} from '../../../data/no-code-data-type';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => api}));
const confirm = vi.hoisted(() => vi.fn());
vi.mock('../../../providers/confirm-provider', () => ({useConfirm: () => confirm}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../editors', () => ({editors: {}}));
vi.mock('../../code-editor/monaco-editor', () => ({MonacoEditor: () => <div/>}));
vi.mock('../../../modules/javascript/javascript-api-service', () => ({
    JavascriptApiService: class {
        getTypes() {
            return Promise.resolve('');
        }
    },
}));
const legacy = {
    type: 'ConditionSet' as const,
    requirements: 'Bestehende Anforderung',
    noCode: null,
    noCodeList: null,
    javascriptCode: null,
    referencedIds: ['field'],
    conditionSet: {
        operator: ConditionSetOperator.Any,
        conditionSetUnmetMessage: 'Gruppe nicht erfüllt',
        conditions: [{
            operator: ConditionOperator.Equals,
            reference: 'field',
            value: 'alt',
            conditionUnmetMessage: 'Einzelne Bedingung nicht erfüllt',
        }],
        conditionsSets: [{
            operator: ConditionSetOperator.All,
            conditionSetUnmetMessage: 'Untergruppe nicht erfüllt',
            conditions: [],
        }],
    },
};
const operator = {
    identifier: 'always-true',
    label: 'Immer wahr',
    packageName: 'core',
    tags: [],
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [],
    }],
};

function tab(section: string) {
    act(() => {
        window.history.replaceState({}, '', `/#field/${section}`);
        window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
}

function setup(section: LegacyNoCodeSection = 'visibility', editable = true, overrides: Partial<TextFieldElement> = {}, initialDraft: ElementPanelDraft | null = null) {
    const initial: TextFieldElement = {
        ...generateElementWithDefaultValues(ElementType.Text),
        id: 'field',
        label: 'Vorname',
        [section]: structuredClone(legacy), ...overrides,
    };
    const form = {
        ...(generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement),
        id: 'form',
        children: [initial],
    } as unknown as FormLayoutElement;
    const onApply = vi.fn();
    const onCancel = vi.fn();
    const onDraft = vi.fn<(draft: ElementPanelDraft | null) => void>();

    function Harness() {
        const [element, setElement] = useState(initial);
        const [panelDraft, setPanelDraft] = useState(initialDraft);
        return <ElementTreeContextProvider value={{
            root: form,
            panelDraft,
            onPanelDraftChange: (draft) => { onDraft(draft); setPanelDraft(draft); },
            editable,
            displayContext: ElementDisplayContext.CustomerFacing,
            allowElementIdEditing: false,
            allElements: [{
                element,
                parents: [form],
            }],
            newElementIds: new Set(),
            scrollToElement: () => {
            },
            canDropElement: () => false,
            moveElement: () => {
            },
            expandCommand: {
                type: null,
                version: 0,
            },
            initiallyExpandedSectionIds: new Set(),
        }}>
            <ElementTreeEditor value={element}
                               parents={[form]}
                               open
                               onClone={() => {
                               }}
                               onDelete={() => {
                               }}
                               onCancel={onCancel}
                               onChange={(next) => {
                                   onApply(next);
                                   setElement(next);
                               }}/>
        </ElementTreeContextProvider>;
    }

    window.history.replaceState({}, '', `/#field/${section}`);
    const {unmount} = render(<Harness/>);
    return {
        onApply,
        onCancel,
        onDraft,
        unmount,
    };
}

async function fillDraft() {
    fireEvent.click(await screen.findByRole('button', {name: 'Ausdruck hinzufügen'}));
    fireEvent.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
    const picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
    fireEvent.click(within(picker).getByRole('button', {name: 'Immer wahr: Hinzufügen'}));
    await waitFor(() => expect(screen.queryByRole('dialog', {name: 'Funktion auswählen'})).not.toBeInTheDocument());
}

beforeEach(() => {
    api.get.mockReset().mockResolvedValue([operator]);
    confirm.mockReset().mockResolvedValue(true);
    vi.spyOn(ElementsApiService.prototype, 'recalculateReferencedIds').mockImplementation(async (element) => element);
});

it.each(['visibility', 'value'] as const)('places the %s toolbar beside the function title and explains its empty state', async (section) => {
    setup('visibility', true, {
        [section]: {
            type: 'NoCode',
            noCode: null,
        },
    });
    if (section === 'value') tab('value');
    await screen.findByRole('button', {name: 'Ausdruck hinzufügen'});
    const header = screen.getByRole('group', {name: 'Funktion'});
    expect(within(header).getByRole('heading', {name: 'Funktion'})).toBeInTheDocument();
    expect(header).toContainElement(screen.getByRole('button', {name: 'Notizen'}));
    expect(header).toContainElement(screen.getByRole('button', {name: 'Testlauf'}));
    expect(header).toContainElement(screen.getByRole('button', {name: 'Rückgängig'}));
    expect(screen.getByRole('treeitem')).toHaveAccessibleDescription(section === 'visibility'
        ? 'Legen Sie fest, unter welchen Bedingungen dieses Element sichtbar ist.'
        : 'Legen Sie fest, wie der Wert dieses Elements berechnet oder aus anderen Daten übernommen wird.');
});

it('keeps each validation expression toolbar with its own condition', async () => {
    setup('validation', true, {
        validation: {
            ...legacy,
            type: 'NoCode',
            conditionSet: null,
            noCodeList: [
                {
                    message: 'Erste Meldung',
                    noCode: null,
                }, {
                    message: 'Zweite Meldung',
                    noCode: null,
                },
            ],
        },
    });
    await waitFor(() => expect(screen.getAllByRole('button', {name: 'Ausdruck hinzufügen'})).toHaveLength(2));
    const notes = screen.getAllByRole('button', {name: 'Notizen'});
    expect(notes).toHaveLength(2);
    for (const [index, message] of ['Erste Meldung', 'Zweite Meldung'].entries()) {
        const group = screen.getByRole('region', {name: `Validierung ${index + 1}`});
        expect(within(group).getByDisplayValue(message)).toBeInTheDocument();
        expect(within(group).getByRole('button', {name: 'Ausdruck hinzufügen'})).toBeInTheDocument();
        expect(within(group).getByRole('button', {name: 'Ausdruck löschen'})).toBeInTheDocument();
        expect(group).toContainElement(notes[index]);
    }
    for (const button of notes) expect(button.parentElement).toHaveTextContent('Bedingung');
    fireEvent.click(screen.getAllByRole('button', {name: 'Testlauf'})[1]);
    expect(screen.getAllByRole('button', {name: 'Testlauf'})[0]).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getAllByRole('button', {name: 'Testlauf'})[1]).toHaveAttribute('aria-expanded', 'true');
});

it.each(['visibility', 'value', 'validation'] as const)('places the Low-Code lookup action in the %s function header', async (section) => {
    setup('visibility', true, {
        [section]: {
            ...legacy,
            type: 'Javascript',
            conditionSet: null,
            javascriptCode: {code: 'return true;'},
        },
    });
    if (section !== 'visibility') tab(section);
    const action = await screen.findByRole('button', {name: 'Element-ID nachschlagen'});
    expect(screen.getByRole('group', {name: 'Funktion'})).toContainElement(action);
    expect(screen.getAllByRole('button', {name: 'Element-ID nachschlagen'})).toHaveLength(1);
    expect(screen.getByRole('group', {name: 'Code-Editor'}).previousElementSibling).toBeNull();
    fireEvent.click(action);
    expect(await screen.findByRole('dialog', {name: 'Element auswählen'})).toBeInTheDocument();
});

describe('manual legacy rule conversion', () => {
    it('blocks an empty conversion and preserves the old rule on cancellation alongside unrelated changes', async () => {
        const {onApply} = setup();
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
        expect(onApply).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(await screen.findByText('Vervollständigen Sie den No-Code-Ausdruck, bevor Sie das Element übernehmen.')).toBeInTheDocument();
        tab('properties');
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Anderer Titel'}});
        tab('visibility');
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung abbrechen'}));
        await screen.findByRole('button', {name: 'Umstellung beginnen'});
        expect(confirm).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
        expect(onApply.mock.calls[0][0]).toMatchObject({
            label: 'Anderer Titel',
            visibility: legacy,
        });
    });
    it.each(['visibility', 'validation'] as const)('keeps the %s draft across tabs and replaces only on explicit apply', async (section) => {
        const {onApply} = setup(section);
        if (section === 'validation') {
            for (const message of ['Gruppe nicht erfüllt', 'Untergruppe nicht erfüllt', 'Einzelne Bedingung nicht erfüllt']) expect(screen.getByText(message)).toBeInTheDocument();
            expect(within(screen.getByRole('region', {name: 'Bisherige Regel Nur lesbar'})).queryByRole('combobox')).not.toBeInTheDocument();
        }
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
        await fillDraft();
        tab('properties');
        tab(section);
        expect(screen.getByRole('button', {name: 'Umstellung abbrechen'})).toBeInTheDocument();
        expect(onApply).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
        expect(onApply.mock.calls[0][0][section]).toMatchObject({
            type: 'NoCode',
            conditionSet: null,
            requirements: legacy.requirements,
        });
        const fn = onApply.mock.calls[0][0][section];
        expect(section === 'visibility' ? fn.noCode : fn.noCodeList[0].noCode).toMatchObject({operatorIdentifier: 'always-true'});
    });
    it('confirms both edited conversion cancellation and closing, without applying either draft', async () => {
        const {
            onApply,
            onCancel,
        } = setup();
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
        await fillDraft();
        confirm.mockResolvedValueOnce(false);
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung abbrechen'}));
        await waitFor(() => expect(confirm).toHaveBeenCalledOnce());
        expect(confirm).toHaveBeenLastCalledWith(expect.objectContaining({isDestructive: true}));
        expect(screen.getByRole('button', {name: 'Umstellung abbrechen'})).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        await waitFor(() => expect(onCancel).toHaveBeenCalledOnce());
        expect(confirm).toHaveBeenCalledTimes(2);
        expect(onApply).not.toHaveBeenCalled();
    });
    it('shows old rules read-only and does not offer conversion without editing rights', () => {
        setup('validation', false);
        expect(screen.getByText(/Ab Prosuna 5.1/)).toBeInTheDocument();
        expect(screen.getByText('Gruppe nicht erfüllt')).toBeVisible();
        expect(within(screen.getByRole('region', {name: 'Bisherige Regel Nur lesbar'})).queryByRole('combobox')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Umstellung beginnen'})).not.toBeInTheDocument();
    });
});

it('focuses the first incomplete validation when multiple expressions fail on apply', async () => {
    const {onApply} = setup('validation');
    fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
    await screen.findByRole('button', {name: 'Ausdruck hinzufügen'});
    fireEvent.click(screen.getByRole('button', {name: 'Validierung hinzufügen'}));
    await waitFor(() => expect(screen.getAllByRole('treeitem')).toHaveLength(2));
    expect(screen.getAllByRole('treeitem').every((item) => item.getAttribute('aria-invalid') !== 'true')).toBe(true);
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    await waitFor(() => expect(screen.getAllByRole('treeitem')[0]).toHaveFocus());
    expect(screen.getAllByRole('treeitem').every((item) => item.getAttribute('aria-invalid') === 'true')).toBe(true);
    expect(onApply).not.toHaveBeenCalled();
});

it('focuses adding a validation when the selected No-Code mode has no expressions left', async () => {
    const {onApply} = setup('validation');
    fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
    await screen.findByRole('button', {name: 'Ausdruck hinzufügen'});
    fireEvent.click(screen.getByRole('button', {name: 'Ausdruck löschen'}));
    expect(screen.queryByRole('treeitem')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    await waitFor(() => expect(screen.getByRole('button', {name: 'Validierung hinzufügen'})).toHaveFocus());
    expect(onApply).not.toHaveBeenCalled();
});


it('restores a pending migration with its old rule and applies only its completed replacement', async () => {
    const first = setup();
    fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
    await fillDraft();
    tab('properties');
    fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Neuer Titel'}});
    const saved = JSON.parse(JSON.stringify(first.onDraft.mock.lastCall![0])) as ElementPanelDraft;
    expect(saved.mine.visibility?.type).toBe('NoCode');
    expect(saved.base.visibility).toEqual(legacy);
    expect(first.onApply).not.toHaveBeenCalled();
    first.unmount();

    const restored = setup('visibility', true, {}, saved);
    expect(await screen.findByRole('button', {name: 'Umstellung abbrechen'})).toBeInTheDocument();
    expect(within(screen.getByRole('region', {name: 'Bisherige Regel Nur lesbar'})).getByText('alt')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    await waitFor(() => expect(restored.onApply).toHaveBeenCalledOnce());
    expect(restored.onApply.mock.calls[0][0]).toMatchObject({
        label: 'Neuer Titel', visibility: {type: 'NoCode', conditionSet: null, noCode: {operatorIdentifier: 'always-true'}},
    });
});

it('still blocks an incomplete migration after recovery and restores the old rule on cancellation', async () => {
    const first = setup();
    fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
    const saved = JSON.parse(JSON.stringify(first.onDraft.mock.lastCall![0])) as ElementPanelDraft;
    first.unmount();
    const restored = setup('visibility', true, {}, saved);
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    expect(await screen.findByText('Vervollständigen Sie den No-Code-Ausdruck, bevor Sie das Element übernehmen.')).toBeInTheDocument();
    expect(restored.onApply).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Umstellung abbrechen'}));
    await screen.findByRole('button', {name: 'Umstellung beginnen'});
    expect(restored.onDraft).toHaveBeenLastCalledWith(null);
});


it('does not apply a migration after an edit supersedes its pending validation', async () => {
    const {onApply} = setup();
    fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
    await fillDraft();
    let resolve!: (operators: typeof operator[]) => void;
    api.get.mockImplementationOnce(() => new Promise(done => { resolve = done; }));
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    await waitFor(() => expect(resolve).toBeTypeOf('function'));
    tab('properties');
    fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Noch bearbeitet'}});
    await act(async () => resolve([operator]));
    expect(onApply).not.toHaveBeenCalled();
    expect(ElementsApiService.prototype.recalculateReferencedIds).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
    await waitFor(() => expect(onApply).toHaveBeenCalledOnce());
    expect(onApply.mock.calls[0][0]).toMatchObject({label: 'Noch bearbeitet', visibility: {type: 'NoCode'}});
});

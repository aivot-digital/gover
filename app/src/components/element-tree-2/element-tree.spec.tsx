import React, {type PropsWithChildren, StrictMode, useState} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {MemoryRouter} from 'react-router-dom';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {type AnyElement} from '../../models/elements/any-element';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type StepperLayoutElement} from '../../models/elements/form/layout/stepper-layout-element';
import {type AnyFormElement} from '../../models/elements/form/any-form-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {DragAndDropProvider} from '../../providers/drag-and-drop-provider';
import {ElementTree} from './element-tree';
import {ElementsApiService} from '../../modules/elements/elements-api-service';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../modules/process/pages/details/components/process-node-editor/process-node-editor-context', () => ({
    useOptionalProcessNodeEditorContext: () => null,
}));
vi.mock('./components/element-tree-editor', () => ({ElementTreeEditor: () => null}));

beforeEach(() => {
    window.history.replaceState(null, '', '/');
});

describe('ElementTree section expansion', () => {
    it('keeps existing sections collapsed when loading a form', () => {
        render(<TestTree value={createForm([
            createSection('existing'),
            createIntroduction(),
        ])}/>, {wrapper: TreeTestWrapper});

        expect(screen.getAllByRole('button', {name: 'Ausklappen'})).toHaveLength(2);
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it.each(['Standardabschnitt', 'Allgemeine Informationen'])(
        'opens a newly added %s from the tree dialog',
        async (sectionLabel) => {
            const user = userEvent.setup();
            render(<EditableTree initialValue={createForm([createSection('existing')])}/>, {wrapper: TreeTestWrapper});

            await user.click(screen.getByRole('button', {name: 'Neuen Abschnitt hinzufügen'}));
            const dialog = screen.getByRole('dialog', {name: 'Formularabschnitt hinzufügen'});
            const optionRow = within(dialog).getByText(sectionLabel).closest('div')!.parentElement!.parentElement!;
            await user.click(within(optionRow).getByRole('button', {name: 'Hinzufügen'}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

            expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Neues Element hinzufügen'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        },
    );

    it('opens multiple sections supplied through external form changes, including empty sections', () => {
        const initial = createForm([createSection('existing')]);
        const {rerender} = render(<TestTree value={initial}/>, {wrapper: TreeTestWrapper});

        rerender(<TestTree value={createForm([
            ...initial.children ?? [],
            createSection('new'),
            createIntroduction(),
            generateElementWithDefaultValues(ElementType.SummaryStep),
            generateElementWithDefaultValues(ElementType.SubmitStep),
        ])}/>);

        expect(screen.getAllByRole('button', {name: 'Einklappen'})).toHaveLength(2);
        expect(screen.getAllByRole('button', {name: 'Neues Element hinzufügen'})).toHaveLength(2);
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
    });

    it('opens new sections under StrictMode', () => {
        const {rerender} = render(<StrictMode><TestTree value={createForm()}/></StrictMode>, {wrapper: TreeTestWrapper});

        rerender(<StrictMode><TestTree value={createForm([createSection('new')])}/></StrictMode>);

        expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Neues Element hinzufügen'})).toBeInTheDocument();
    });

    it('keeps nested groups collapsed inside a newly added section', () => {
        const initial = createForm();
        const {rerender} = render(<TestTree value={initial}/>, {wrapper: TreeTestWrapper});

        rerender(<TestTree value={createForm([
            createSection('new', [createGroup('nested', [createField('field', 'Gruppendaten')])]),
        ])}/>);

        expect(screen.getByText('nested')).toBeInTheDocument();
        expect(screen.queryByText('Gruppendaten')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
    });

    it('does not reopen a manually collapsed new section on subsequent edits', async () => {
        const user = userEvent.setup();
        const {rerender} = render(<TestTree value={createForm()}/>, {wrapper: TreeTestWrapper});
        const section = createSection('new');
        rerender(<TestTree value={createForm([section])}/>);
        await user.click(screen.getByRole('button', {name: 'Einklappen'}));

        rerender(<TestTree value={createForm([{...section, title: 'Umbenannter Abschnitt'}])}/>);

        expect(screen.getByText('Umbenannter Abschnitt')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('preserves section expansion when reordering existing sections', async () => {
        const user = userEvent.setup();
        const first = createSection('first', [createField('firstField', 'Erster Inhalt')]);
        const second = createSection('second', [createField('secondField', 'Zweiter Inhalt')]);
        const {rerender} = render(<TestTree value={createForm([first, second])}/>, {wrapper: TreeTestWrapper});
        await user.click(screen.getAllByRole('button', {name: 'Ausklappen'})[0]);

        rerender(<TestTree value={createForm([second, first])}/>);

        expect(screen.getByText('Erster Inhalt')).toBeInTheDocument();
        expect(screen.queryByText('Zweiter Inhalt')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
    });

    it('does not treat a restored section as newly added after undo and redo', async () => {
        const user = userEvent.setup();
        const emptyForm = createForm();
        const addedSection = createSection('new');
        const {rerender} = render(<TestTree value={emptyForm}/>, {wrapper: TreeTestWrapper});
        rerender(<TestTree value={createForm([addedSection])}/>);
        await user.click(screen.getByRole('button', {name: 'Einklappen'}));

        rerender(<TestTree value={emptyForm}/>);
        rerender(<TestTree value={createForm([addedSection])}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('keeps the sections of a different form collapsed when switching roots', () => {
        const {rerender} = render(<TestTree value={createForm()}/>, {wrapper: TreeTestWrapper});

        rerender(<TestTree value={{...createForm([createSection('otherSection')]), id: 'otherForm'}}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('does not automatically expand sections received in read-only mode', () => {
        const {rerender} = render(<TestTree value={createForm()} editable={false}/>, {wrapper: TreeTestWrapper});
        const updated = createForm([createSection('new')]);
        rerender(<TestTree value={updated} editable={false}/>);
        rerender(<TestTree value={updated}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('does not apply form section defaults to other UI definition trees', () => {
        const initial: StepperLayoutElement = {
            ...createGroup('stepper', []),
            type: ElementType.StepperLayout,
            children: [],
        };
        const {rerender} = render(<TestTree value={initial}/>, {wrapper: TreeTestWrapper});

        rerender(<TestTree value={{...initial, children: [createSection('new')]}}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Neues Element hinzufügen'})).toHaveLength(1);
    });

    it.each(['Elemente einklappen', 'Elemente ausklappen'])(
        'keeps the new section open and its groups collapsed after a previous "%s" command',
        async (command) => {
            const user = userEvent.setup();
            const initial = createForm([
                createSection('existing', [createGroup('existingGroup', [createField('existingField', 'Bestehende Gruppendaten')])]),
            ]);
            const {rerender} = render(<TestTree value={initial}/>, {wrapper: TreeTestWrapper});
            await user.click(screen.getByRole('button', {name: command}));
            if (command === 'Elemente ausklappen') {
                expect(screen.getByText('Bestehende Gruppendaten')).toBeInTheDocument();
            }

            rerender(<TestTree value={createForm([
                ...initial.children ?? [],
                createSection('new', [createGroup('newGroup', [createField('newField', 'Neue Gruppendaten')])]),
            ])}/>);

            expect(screen.getByText('newGroup')).toBeInTheDocument();
            expect(screen.queryByText('Neue Gruppendaten')).not.toBeInTheDocument();
            const existingOpen = command === 'Elemente ausklappen';
            expect(screen.getAllByRole('button', {name: 'Einklappen'})).toHaveLength(existingOpen ? 3 : 1);
            expect(screen.getAllByRole('button', {name: 'Ausklappen'})).toHaveLength(existingOpen ? 1 : 2);

            await user.click(screen.getByRole('button', {name: 'Elemente ausklappen'}));
            expect(screen.getByText('Neue Gruppendaten')).toBeInTheDocument();
            await user.click(screen.getByRole('button', {name: 'Elemente einklappen'}));
            expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
        },
    );
});

describe('ElementTree configuration errors', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('marks sections containing erroneous elements and reveals them on request', async () => {
        const message = 'Sie fordern mehr Zeichen, als Sie maximal zulassen.';
        vi.spyOn(ElementsApiService.prototype, 'validateConfiguration').mockResolvedValue({
            properties: {},
            children: {
                section: {
                    properties: {},
                    children: {
                        field: {properties: {minCharacters: [message], maxCharacters: [message]}, children: {}},
                    },
                },
            },
        });
        const user = userEvent.setup();
        render(<TestTree value={createForm([
            createSection('section', [createField('field', 'Vornamen')]),
        ])}/>, {wrapper: TreeTestWrapper});
        const fieldMarker = {name: `Fehlerhafte Konfiguration: ${message}`};

        const sectionMarker = await screen.findByRole('button', {name: 'Enthält Elemente mit fehlerhafter Konfiguration'});
        expect(screen.queryByRole('button', fieldMarker)).not.toBeInTheDocument();

        await user.click(sectionMarker);

        expect(await screen.findByRole('button', fieldMarker)).toBeInTheDocument();
    });

    it('does not validate a read-only tree', async () => {
        const validate = vi.spyOn(ElementsApiService.prototype, 'validateConfiguration');
        render(<TestTree value={createForm([createSection('section')])} editable={false}/>, {wrapper: TreeTestWrapper});

        await act(() => new Promise((resolve) => setTimeout(resolve, 500)));

        expect(validate).not.toHaveBeenCalled();
    });

    it('shows errors validated by the host without validating the tree again', async () => {
        const validate = vi.spyOn(ElementsApiService.prototype, 'validateConfiguration');
        render(<ElementTree
            value={createForm([createSection('section')])}
            onChange={vi.fn()}
            editable={true}
            displayContext={ElementDisplayContext.CustomerFacing}
            allowElementIdEditing={false}
            configurationErrors={{properties: {}, children: {section: {properties: {title: ['Fehler']}, children: {}}}}}
        />, {wrapper: TreeTestWrapper});

        expect(screen.getByRole('button', {name: 'Fehlerhafte Konfiguration: Fehler'})).toBeInTheDocument();
        await act(() => new Promise((resolve) => setTimeout(resolve, 500)));
        expect(validate).not.toHaveBeenCalled();
    });
});

describe('ElementTree drag and drop', () => {
    it('validates drops against the current display context', async () => {
        const onChange = vi.fn();
        const group = createGroup('group', [createHeadline('headline', 'Überschrift'), createField('field', 'Feld')]);
        const {rerender} = render(<TestTree value={group} onChange={onChange}/>, {wrapper: TreeTestWrapper});

        await dragToSlot('Überschrift', 'group', 2);
        expect(onChange).toHaveBeenCalledTimes(1);
        expect((onChange.mock.calls[0][0] as GroupLayout).children.map(child => child.id)).toEqual(['field', 'headline']);

        // Data object schemas do not accept headlines inside groups.
        rerender(<TestTree value={group} onChange={onChange} displayContext={ElementDisplayContext.DataObjectSchema}/>);
        await dragToSlot('Überschrift', 'group', 2);
        expect(onChange).toHaveBeenCalledTimes(1);
    });
});

function TreeTestWrapper({children}: PropsWithChildren) {
    return <DragAndDropProvider>
        <MemoryRouter>{children}</MemoryRouter>
    </DragAndDropProvider>;
}

function TestTree({
    value,
    editable = true,
    onChange = vi.fn(),
    displayContext = ElementDisplayContext.CustomerFacing,
}: {
    value: AnyElement;
    editable?: boolean;
    onChange?: (value: AnyElement) => void;
    displayContext?: ElementDisplayContext;
}) {
    return <ElementTree
        value={value}
        onChange={onChange}
        editable={editable}
        displayContext={displayContext}
        allowElementIdEditing={false}
    />;
}

function EditableTree({initialValue}: {initialValue: FormLayoutElement}) {
    const [value, setValue] = useState(initialValue);

    return <TestTree value={value} onChange={(updated) => setValue(updated as FormLayoutElement)}/>;
}

function createForm(children: AnyElement[] = []): FormLayoutElement {
    return {...generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement, id: 'form', children};
}

function createSection(id: string, children: AnyFormElement[] = []): StepElement {
    return {...generateElementWithDefaultValues(ElementType.Step), id, title: id, children};
}

function createIntroduction(): AnyElement {
    return {...generateElementWithDefaultValues(ElementType.IntroductionStep), id: 'introduction'};
}

function createGroup(id: string, children: AnyFormElement[]): GroupLayout {
    return {...generateElementWithDefaultValues(ElementType.GroupLayout), id, name: id, children};
}

function createField(id: string, label: string): AnyFormElement {
    return {...generateElementWithDefaultValues(ElementType.Text), id, label};
}

function createHeadline(id: string, content: string): AnyFormElement {
    return {...generateElementWithDefaultValues(ElementType.Headline), id, content};
}

async function dragToSlot(label: string, parentId: string, targetIndex: number) {
    // React DnD marks connected drag sources as draggable.
    const source = screen.getByText(label).closest('[draggable="true"]');
    expect(source).not.toBeNull();
    const target = screen.getByTestId(`element-tree-drop-slot-${parentId}.${targetIndex}`);
    const event = {dataTransfer: {types: [], setData: vi.fn()}, clientX: 0, clientY: 1};
    fireEvent.dragStart(source!, event);
    fireEvent.dragEnter(target, event);
    fireEvent.dragOver(target, event);
    fireEvent.drop(target, event);
    fireEvent.dragEnd(source!, event);
    await act(async () => {});
}
